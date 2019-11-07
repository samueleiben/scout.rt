/*******************************************************************************
 * Copyright (c) 2014-2015 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
scout.ImageFieldLayout = function(imageField) {
  scout.ImageFieldLayout.parent.call(this, imageField);
};
scout.inherits(scout.ImageFieldLayout, scout.FormFieldLayout);

scout.ImageFieldLayout.prototype.layout = function($container) {
  scout.ImageFieldLayout.parent.prototype.layout.call(this, $container);
  scout.scrollbars.update(this.formField.$fieldContainer);
};

scout.ImageFieldLayout.prototype.naturalSize = function(formField) {
  if (formField.image && formField.image.htmlComp) {
    return formField.image.htmlComp.getPreferredSize()
      .add(formField.image.htmlComp.getMargins());
  }
  scout.ImageFieldLayout.parent.prototype.naturalSize.call(this, formField);
};