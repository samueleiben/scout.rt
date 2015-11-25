/*******************************************************************************
 * Copyright (c) 2015 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.client.services.common.notifications;

import static org.mockito.Mockito.mock;

import java.io.Serializable;
import java.util.List;

import org.eclipse.scout.commons.IRunnable;
import org.eclipse.scout.rt.client.IClientSession;
import org.eclipse.scout.rt.client.clientnotification.ClientNotificationDispatcher;
import org.eclipse.scout.rt.client.context.ClientRunContexts;
import org.eclipse.scout.rt.client.testenvironment.TestEnvironmentClientSession;
import org.eclipse.scout.rt.platform.BEANS;
import org.eclipse.scout.rt.platform.BeanMetaData;
import org.eclipse.scout.rt.platform.IBean;
import org.eclipse.scout.rt.platform.IBeanManager;
import org.eclipse.scout.rt.platform.IgnoreBean;
import org.eclipse.scout.rt.platform.job.DoneEvent;
import org.eclipse.scout.rt.platform.job.IBlockingCondition;
import org.eclipse.scout.rt.platform.job.IDoneHandler;
import org.eclipse.scout.rt.platform.job.Jobs;
import org.eclipse.scout.rt.shared.notification.INotificationHandler;
import org.eclipse.scout.rt.shared.notification.NotificationHandlerRegistry;
import org.eclipse.scout.rt.testing.client.runner.ClientTestRunner;
import org.eclipse.scout.rt.testing.client.runner.RunWithClientSession;
import org.eclipse.scout.rt.testing.platform.runner.RunWithSubject;
import org.eclipse.scout.rt.testing.shared.TestingUtility;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

@RunWith(ClientTestRunner.class)
@RunWithClientSession(TestEnvironmentClientSession.class)
@RunWithSubject("anna")
public class NotificationDispatcherTest {

  private List<IBean<?>> m_serviceReg;
  private volatile GlobalNotificationHandler m_globalNotificationHanlder;
  private volatile GroupNotificationHandler m_groupNotificationHanlder;

  @Before
  public void before() throws Exception {
    m_globalNotificationHanlder = mock(GlobalNotificationHandler.class);
    m_groupNotificationHanlder = mock(GroupNotificationHandler.class);
    m_serviceReg = TestingUtility.registerBeans(
        new BeanMetaData(GlobalNotificationHandler.class).withInitialInstance(m_globalNotificationHanlder).withApplicationScoped(true),
        new BeanMetaData(GroupNotificationHandler.class).withInitialInstance(m_groupNotificationHanlder).withApplicationScoped(true));

    // ensure bean hander cache of notification dispatcher gets refreshed
    IBeanManager beanManager = BEANS.getBeanManager();
    IBean<NotificationHandlerRegistry> bean = beanManager.getBean(NotificationHandlerRegistry.class);
    beanManager.unregisterBean(bean);
    beanManager.registerBean(new BeanMetaData(bean));
  }

  @After
  public void after() {
    TestingUtility.unregisterBeans(m_serviceReg);
    // ensure bean hander cache of notification dispatcher gets refreshed
    IBeanManager beanManager = BEANS.getBeanManager();
    IBean<NotificationHandlerRegistry> bean = beanManager.getBean(NotificationHandlerRegistry.class);
    beanManager.unregisterBean(bean);
    beanManager.registerBean(new BeanMetaData(bean));
  }

  @Test
  public void testStringNotification() {
    final IBlockingCondition cond = Jobs.getJobManager().createBlockingCondition("Suspend JUnit model thread", true);
    final String stringNotification = "A simple string notification";

    Jobs.schedule(new IRunnable() {
      @Override
      public void run() throws Exception {
        final ClientNotificationDispatcher dispatcher = BEANS.get(ClientNotificationDispatcher.class);
        dispatcher.dispatch((IClientSession) IClientSession.CURRENT.get(), stringNotification);
        dispatcher.waitForPendingNotifications();
      }
    }, Jobs.newInput()
        .withRunContext(ClientRunContexts.copyCurrent()))
        .whenDone(new IDoneHandler<Void>() {
          @Override
          public void onDone(DoneEvent<Void> event) {
            cond.setBlocking(false);
          }
        }, null);
    cond.waitFor();
    Mockito.verify(m_globalNotificationHanlder, Mockito.times(1)).handleNotification(Mockito.any(Serializable.class));
    Mockito.verify(m_groupNotificationHanlder, Mockito.times(0)).handleNotification(Mockito.any(INotificationGroup.class));
  }

  @Test
  public void testSuperClassNotification() {
    final IBlockingCondition cond = Jobs.getJobManager().createBlockingCondition("Suspend JUnit model thread1", true);

    Jobs.schedule(new IRunnable() {
      @Override
      public void run() throws Exception {
        ClientNotificationDispatcher dispatcher = BEANS.get(ClientNotificationDispatcher.class);
        dispatcher.dispatch((IClientSession) IClientSession.CURRENT.get(), new Notification01());
        dispatcher.dispatch((IClientSession) IClientSession.CURRENT.get(), new Notification02());
        dispatcher.dispatch((IClientSession) IClientSession.CURRENT.get(), new Notification02());
        dispatcher.waitForPendingNotifications();
      }
    }, Jobs.newInput()
        .withRunContext(ClientRunContexts.copyCurrent()))
        .whenDone(new IDoneHandler<Void>() {
          @Override
          public void onDone(DoneEvent<Void> event) {
            cond.setBlocking(false);
          }
        }, null);
    cond.waitFor();
    Mockito.verify(m_globalNotificationHanlder, Mockito.times(3)).handleNotification(Mockito.any(Serializable.class));
    Mockito.verify(m_groupNotificationHanlder, Mockito.times(2)).handleNotification(Mockito.any(INotificationGroup.class));
  }

  @IgnoreBean
  private static class GlobalNotificationHandler implements INotificationHandler<Serializable> {

    @Override
    public void handleNotification(Serializable notification) {
    }
  }

  private static class GroupNotificationHandler implements INotificationHandler<INotificationGroup> {

    @Override
    public void handleNotification(INotificationGroup notification) {
    }
  }

  private static final class Notification01 implements Serializable {

    private static final long serialVersionUID = 1L;
  }

  public static interface INotificationGroup extends Serializable {

  }

  private static final class Notification02 implements INotificationGroup {

    private static final long serialVersionUID = 1L;
  }
}
