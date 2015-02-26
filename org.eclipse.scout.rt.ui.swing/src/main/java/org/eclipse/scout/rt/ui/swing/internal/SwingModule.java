/*******************************************************************************
 * Copyright (c) 2010 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.ui.swing.internal;

import java.util.Hashtable;

import org.eclipse.core.runtime.Platform;
import org.eclipse.scout.rt.platform.IModule;
import org.eclipse.scout.rt.platform.cdi.ApplicationScoped;
import org.eclipse.scout.rt.ui.swing.login.internal.InternalNetAuthenticator;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;

@ApplicationScoped
public class SwingModule implements IModule {
  private ServiceRegistration m_netAuthRegistration;

  @Override
  public void start() {
    // register net authenticator ui
    Hashtable<String, Object> map = new Hashtable<String, Object>();
    map.put(Constants.SERVICE_RANKING, -2);
    if (Platform.isRunning()) {
      //TODO imo what to do in java rt without osgi? simply register java.net.Authenticator in META-INF/service?
      m_netAuthRegistration = FrameworkUtil.getBundle(getClass()).getBundleContext().registerService(java.net.Authenticator.class.getName(), new InternalNetAuthenticator(), map);
    }
  }

  @Override
  public void stop() {
    if (m_netAuthRegistration != null) {
      m_netAuthRegistration.unregister();
      m_netAuthRegistration = null;
    }
  }
}
