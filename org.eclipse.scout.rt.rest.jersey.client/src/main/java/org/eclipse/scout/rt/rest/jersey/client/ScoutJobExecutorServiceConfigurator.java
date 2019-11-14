/*******************************************************************************
 * Copyright (c) 2010-2018 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.rest.jersey.client;

import javax.ws.rs.client.ClientBuilder;

import org.eclipse.scout.rt.rest.client.IGlobalRestClientConfigurator;

public class ScoutJobExecutorServiceConfigurator implements IGlobalRestClientConfigurator {

  @Override
  public void configure(ClientBuilder clientBuilder) {
    clientBuilder.register(ScoutJobExecutorServiceProvider.class);
  }
}