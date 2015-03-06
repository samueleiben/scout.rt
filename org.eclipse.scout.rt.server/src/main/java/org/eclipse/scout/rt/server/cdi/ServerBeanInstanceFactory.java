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
package org.eclipse.scout.rt.server.cdi;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

import org.eclipse.scout.rt.platform.IPlatform;
import org.eclipse.scout.rt.platform.cdi.IBeanInstanceFactory;
import org.eclipse.scout.rt.platform.cdi.IBeanRegistration;
import org.eclipse.scout.rt.server.Server;

/**
 * Default server-side {@link IBeanInstanceFactory} used in {@link IPlatform#getBeanContext()}
 */
public class ServerBeanInstanceFactory implements IBeanInstanceFactory {

  @Override
  public <T> T select(Class<T> queryClass, SortedSet<IBeanRegistration> regs) {
    for (IBeanRegistration<?> reg : regs) {
      if (reg.getBean().getBeanAnnotation(Server.class) != null) {
        return createServerInterceptor(queryClass, reg);
      }
      else {
        return createDefaultInterceptor(queryClass, reg);
      }
    }
    return null;
  }

  @Override
  public <T> List<T> selectAll(Class<T> queryClass, SortedSet<IBeanRegistration> regs) {
    //TODO imo add context around queryClass (interface)
    ArrayList<T> result = new ArrayList<T>();
    for (IBeanRegistration<?> reg : regs) {
      T instance;
      if (reg.getBean().getBeanAnnotation(Server.class) != null) {
        instance = createServerInterceptor(queryClass, reg);
      }
      else {
        instance = createDefaultInterceptor(queryClass, reg);
      }
      //add
      if (instance != null) {
        result.add(instance);
      }
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  protected <T> T createServerInterceptor(Class<T> queryClass, IBeanRegistration reg) {
    //TODO imo add context around queryClass (interface)
    return (T) reg.getInstance();
  }

  @SuppressWarnings("unchecked")
  protected <T> T createDefaultInterceptor(Class<T> queryClass, IBeanRegistration reg) {
    return (T) reg.getInstance();
  }
}
