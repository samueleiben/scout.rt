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
package org.eclipse.scout.rt.client.mobile.transformation;

import java.util.Collection;
import java.util.List;

import org.eclipse.scout.commons.annotations.OrderedCollection;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.rt.client.session.ClientSessionProvider;
import org.eclipse.scout.rt.client.ui.action.IAction;
import org.eclipse.scout.rt.client.ui.action.menu.IMenu;
import org.eclipse.scout.rt.client.ui.basic.table.ITable;
import org.eclipse.scout.rt.client.ui.desktop.IDesktop;
import org.eclipse.scout.rt.client.ui.desktop.outline.IOutline;
import org.eclipse.scout.rt.client.ui.desktop.outline.pages.IPageWithTable;
import org.eclipse.scout.rt.client.ui.form.IForm;
import org.eclipse.scout.rt.client.ui.form.fields.tabbox.ITabBox;

/**
 * @since 3.9.0
 */
public class TabletDeviceTransformer implements IDeviceTransformer {
  private IDesktop m_desktop;

  public TabletDeviceTransformer() {
    this(null);
  }

  public TabletDeviceTransformer(IDesktop desktop) {
    if (desktop == null) {
      desktop = ClientSessionProvider.currentSession().getDesktop();
    }
    m_desktop = desktop;
    if (m_desktop == null) {
      throw new IllegalArgumentException("No desktop found. Cannot create device transformer.");
    }
  }

  @Override
  public void transformDesktop() throws ProcessingException {
    m_desktop.setCacheSplitterPosition(false);
  }

  @Override
  public void transformForm(IForm form) throws ProcessingException {
  }

  @Override
  public void transformOutline(IOutline outline) throws ProcessingException {
    outline.setBreadcrumbEnabled(true);
  }

  @Override
  public void transformPageDetailTable(ITable table) throws ProcessingException {
  }

  @Override
  public void adaptFormHeaderLeftActions(IForm form, List<IMenu> menuList) {
  }

  @Override
  public void adaptFormHeaderRightActions(IForm form, List<IMenu> menuList) {
  }

  @Override
  public void adaptDesktopActions(Collection<IAction> actions) {
  }

  @Override
  public void adaptDesktopOutlines(OrderedCollection<IOutline> outlines) {
  }

  @Override
  public void notifyDesktopClosing() {
  }

  @Override
  public void notifyTablePageLoaded(IPageWithTable<?> tablePage) throws ProcessingException {
  }

  @Override
  public boolean acceptFormAddingToDesktop(IForm form) {
    return false;
  }

  @Override
  public boolean acceptMobileTabBoxTransformation(ITabBox tabBox) {
    return false;
  }

  @Override
  public List<String> getAcceptedViewIds() {
    return null;
  }

  @Override
  public DeviceTransformationConfig getDeviceTransformationConfig() {
    return null;
  }

}
