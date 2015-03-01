/*******************************************************************************
 * Copyright (c) 2011 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Daniel Wiehl (BSI Business Systems Integration AG) - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.jaxws.internal.tube;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import org.eclipse.scout.commons.Assertions;
import org.eclipse.scout.commons.CollectionUtility;
import org.eclipse.scout.commons.ReflectionUtility;
import org.eclipse.scout.commons.TypeCastUtility;
import org.eclipse.scout.commons.annotations.Internal;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.commons.job.ICallable;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.jaxws.annotation.ScoutTransaction;
import org.eclipse.scout.jaxws.annotation.ScoutWebService;
import org.eclipse.scout.jaxws.internal.ContextHelper;
import org.eclipse.scout.jaxws.internal.SessionHelper;
import org.eclipse.scout.jaxws.security.provider.IAuthenticationHandler;
import org.eclipse.scout.jaxws.security.provider.ICredentialValidationStrategy;
import org.eclipse.scout.jaxws.session.IServerSessionFactory;
import org.eclipse.scout.rt.server.IServerSession;
import org.eclipse.scout.rt.server.job.ServerJobInput;
import org.eclipse.scout.rt.server.job.internal.ServerJobManager;

import com.sun.xml.internal.ws.api.WSBinding;
import com.sun.xml.internal.ws.api.pipe.ClientTubeAssemblerContext;
import com.sun.xml.internal.ws.api.pipe.ServerTubeAssemblerContext;
import com.sun.xml.internal.ws.api.pipe.Tube;
import com.sun.xml.internal.ws.api.pipe.TubelineAssembler;

/**
 * Tube line assembler which installs a security handler at runtime.
 */
@SuppressWarnings("restriction")
public class ScoutTubelineAssembler implements TubelineAssembler {

  private static final IScoutLogger LOG = ScoutLogManager.getLogger(ScoutTubelineAssembler.class);

  private static final Set<Method> TRANSACTIONAL_HANDLER_METHODS = CollectionUtility.hashSet(javax.xml.ws.handler.Handler.class.getDeclaredMethods());

  @Override
  public Tube createClient(ClientTubeAssemblerContext context) {
    // proxy transactional handler to run in a separate transaction.
    WSBinding binding = context.getBinding();
    binding.setHandlerChain(interceptHandlers(binding.getHandlerChain()));

    // assemble tubes
    Tube head = context.createTransportTube();
    head = context.createSecurityTube(head);
    head = context.createWsaTube(head);
    head = context.createClientMUTube(head);
    head = context.createValidationTube(head);
    return context.createHandlerTube(head);
  }

  @Override
  public Tube createServer(ServerTubeAssemblerContext context) {
    // Installs the authentication handler
    // This must precede wrapping handlers with a transactional context
    installServerAuthenticationHandler(context);

    // proxy transactional handler to run in a separate transaction.
    WSBinding binding = context.getEndpoint().getBinding();
    binding.setHandlerChain(interceptHandlers(binding.getHandlerChain()));

    // assemble tubes
    Tube head = context.getTerminalTube();
    head = context.createValidationTube(head);
    head = context.createHandlerTube(head);
    head = context.createMonitoringTube(head);
    head = context.createServerMUTube(head);
    head = context.createWsaTube(head);
    head = context.createSecurityTube(head);
    return head;
  }

  /**
   * Method invoked to change the handlers to be installed.<br/>
   * The default implementation proxies transactional handlers to run on behalf of a new server-job.
   */
  @Internal
  protected List<Handler> interceptHandlers(final List<Handler> handlers) {
    for (int i = 0; i < handlers.size(); i++) {
      final Handler handler = handlers.get(i);
      final ScoutTransaction transactionalAnnotation = getAnnotation(handler.getClass(), ScoutTransaction.class);
      if (transactionalAnnotation != null) {
        handlers.set(i, proxyTransactionalHandler(handler, transactionalAnnotation));
      }
    }
    return handlers;
  }

  /**
   * Method invoked to create a transactional proxy for the given {@link Handler}.
   */
  @Internal
  protected Handler proxyTransactionalHandler(final Handler handler, final ScoutTransaction scoutTransaction) {
    if (scoutTransaction.sessionFactory() == null) {
      LOG.error("Failed to initialize transactional handler because no session-factory configured [handler={}]", handler.getClass().getName());
      return handler;
    }

    final IServerSessionFactory sessionFactory;
    try {
      sessionFactory = scoutTransaction.sessionFactory().newInstance();
    }
    catch (final ReflectiveOperationException e) {
      LOG.error(String.format("Failed to initialize transactional handler because session-factory could not be instantiated [handler=%s, sessionFactory=%s]", handler.getClass().getName(), scoutTransaction.sessionFactory().getName()), e);
      return handler;
    }

    // Create a proxy for the given handler with TX-support.
    return (Handler) Proxy.newProxyInstance(handler.getClass().getClassLoader(), ReflectionUtility.getInterfaces(handler.getClass()), new InvocationHandler() {

      @Override
      public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        if (TRANSACTIONAL_HANDLER_METHODS.contains(method)) {
          Assertions.assertTrue(args.length == 1 && args[0] instanceof MessageContext, "Wrong method signature: %s argument expected. [handler=%s, method=%s]", MessageContext.class.getSimpleName(), handler.getClass().getName(), method.getName());
          final MessageContext messageContext = Assertions.assertNotNull((MessageContext) args[0], "MessageContext must not be null");

          final IServerSession session = lookupServerSession(messageContext, sessionFactory);
          if (session != null) {
            return ScoutTubelineAssembler.this.invokeInServerJob(ServerJobInput.defaults().name("JAX-WS TX-Handler").session(session), handler, method, args);
          }
          else {
            throw new WebServiceException("JAX-WS request rejected because no server-session available.");
          }
        }
        else {
          return method.invoke(handler, args);
        }
      }
    });
  }

  /**
   * Method invoked to run the given method on behalf of a new server job.
   */
  @Internal
  protected Object invokeInServerJob(final ServerJobInput input, final Object object, final Method method, final Object[] args) throws Throwable {
    try {
      return ServerJobManager.DEFAULT.runNow(new ICallable<Object>() {

        @Override
        public Object call() throws Exception {
          try {
            return (Object) method.invoke(object, args); // InvocationTargetException is unpacked in server-job.
          }
          catch (IllegalAccessException | IllegalArgumentException e) {
            throw new WebServiceException("Failed to invoke proxy method", e);
          }
        }
      }, input);
    }
    catch (final ProcessingException e) {
      throw e.getCause(); // propagate the real cause.
    }
  }

  /**
   * Method invoked to lookup the current server session on the message context or if not exists, to create a new one
   * and register it with the context.
   */
  @Internal
  protected IServerSession lookupServerSession(final MessageContext messageContext, final IServerSessionFactory sessionFactory) {
    IServerSession serverSession = ContextHelper.getContextSession(messageContext);
    if (serverSession != null) {
      return serverSession;
    }

    try {
      serverSession = SessionHelper.createNewServerSession(sessionFactory);
      ContextHelper.setContextSession(messageContext, sessionFactory, serverSession);
      return serverSession;
    }
    catch (final RuntimeException e) {
      LOG.error("Failed to create server session for transactional handler", e);
      return null;
    }
  }

  private void installServerAuthenticationHandler(ServerTubeAssemblerContext context) {
    List<Handler> handlerChain = new LinkedList<Handler>();

    // install existing handlers
    handlerChain.addAll(context.getEndpoint().getBinding().getHandlerChain());

    // install authentication handler
    IAuthenticationHandler authenticationHandler = createAuthenticationHandler(context);
    if (authenticationHandler != null) {
      handlerChain.add(authenticationHandler);
    }

    // install handler to put the session factory configured on the port type into the runnning context.
    // This handler must be installed prior to authentication handlers
    Class<?> portTypeClass = context.getEndpoint().getImplementationClass();
    try {
      if (portTypeClass != null) {
        ScoutWebService scoutWebService = portTypeClass.getAnnotation(ScoutWebService.class);
        if (scoutWebService != null) {
          handlerChain.add(new P_PortTypeSessionFactoryRegistrationHandler(scoutWebService));
        }
      }
    }
    catch (Exception e) {
      LOG.error("failed to install handler to register configured port type factory in running context", e);
    }

    // set handler chain
    context.getEndpoint().getBinding().setHandlerChain(handlerChain);
  }

  private IAuthenticationHandler createAuthenticationHandler(ServerTubeAssemblerContext context) {
    Class<?> wsImplClazz = context.getEndpoint().getImplementationClass();
    if (wsImplClazz == null) {
      return null;
    }

    ScoutWebService annotation = getAnnotation(wsImplClazz, ScoutWebService.class);
    if (annotation == null) {
      return null;
    }

    Class<? extends IAuthenticationHandler> authenticationHandlerClazz = annotation.authenticationHandler();
    if (authenticationHandlerClazz == null || authenticationHandlerClazz == IAuthenticationHandler.NONE.class) {
      return null;
    }

    IAuthenticationHandler authenticationHandler = null;
    try {
      authenticationHandler = authenticationHandlerClazz.newInstance();
    }
    catch (Throwable e) {
      LOG.error("Failed to create authentication handler '" + authenticationHandlerClazz.getName() + "'. No authentication is applied.", e);
      return null;
    }

    // inject credential validation strategy
    Class<? extends ICredentialValidationStrategy> strategyClazz = annotation.credentialValidationStrategy();
    if (strategyClazz == null) {
      return authenticationHandler;
    }

    ICredentialValidationStrategy strategy = null;
    try {
      strategy = strategyClazz.newInstance();
    }
    catch (Throwable e) {
      LOG.error("Failed to create credential validation strategy '" + strategyClazz.getName() + "' for authentication handler '" + authenticationHandler.getClass().getName() + "'.", e);
      return authenticationHandler;
    }

    // inject credential validation strategy
    try {
      authenticationHandler.injectCredentialValidationStrategy(strategy);
    }
    catch (Throwable e) {
      LOG.error("Failed to inject credential validation strategy to authentication handler '" + authenticationHandler.getClass().getName() + "'.", e);
      return authenticationHandler;
    }

    return authenticationHandler;
  }

  private <A extends Annotation> A getAnnotation(Class<?> type, Class<A> annotationClazz) {
    A annotation = type.getAnnotation(annotationClazz);
    if (annotation == null && type != Object.class) {
      return getAnnotation(type.getSuperclass(), annotationClazz);
    }
    return annotation;
  }

  /**
   * Handler used to store the session factory configured on the port type in the calling context.
   * This must be the first handler installed.
   */
  private class P_PortTypeSessionFactoryRegistrationHandler implements SOAPHandler<SOAPMessageContext> {

    private ScoutWebService m_scoutWebServiceAnnotation;

    private P_PortTypeSessionFactoryRegistrationHandler(ScoutWebService scoutWebServiceAnnotation) {
      m_scoutWebServiceAnnotation = scoutWebServiceAnnotation;
    }

    @Override
    public boolean handleMessage(SOAPMessageContext context) {
      boolean outbound = TypeCastUtility.castValue(context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY), boolean.class);
      if (outbound) {
        return true; // only inbound messages are of interest
      }
      try {
        // get session factory configured on port type
        IServerSessionFactory portTypeSessionFactory = m_scoutWebServiceAnnotation.sessionFactory().newInstance();
        // store session factory in running context
        ContextHelper.setPortTypeSessionFactory(context, portTypeSessionFactory);
      }
      catch (Exception e) {
        LOG.error("Failed to put port type session factory into the running context", e);
      }
      return true;
    }

    @Override
    public boolean handleFault(SOAPMessageContext context) {
      return true;
    }

    @Override
    public void close(MessageContext context) {
    }

    @Override
    public Set<QName> getHeaders() {
      return new HashSet<QName>();
    }
  }
}
