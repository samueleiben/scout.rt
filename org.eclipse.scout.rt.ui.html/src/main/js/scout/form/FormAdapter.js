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
scout.FormAdapter = function() {
  scout.FormAdapter.parent.call(this);
  this._addAdapterProperties(['rootGroupBox', 'views', 'dialogs', 'initialFocus', 'messageBoxes', 'fileChoosers']);
};
scout.inherits(scout.FormAdapter, scout.ModelAdapter);

scout.FormAdapter.prototype.onModelAction = function(event) {
  if (event.type === 'requestFocus') {
    this._onRequestFocus(event.formField);
  } else {
    scout.FormAdapter.parent.prototype.onModelAction.call(this, event);
  }
};

scout.FormAdapter.prototype._onRequestFocus = function(formFieldId) {
  var formFieldWidget = this.session.getOrCreateWidget(formFieldId, this.widget);
  this.widget.requestFocus(formFieldWidget);
};