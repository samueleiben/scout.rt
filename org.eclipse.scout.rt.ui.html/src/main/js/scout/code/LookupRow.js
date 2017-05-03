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
scout.LookupRow = function() {
  this.key = null;
  this.text = null;
};

scout.LookupRow.prototype.init = function(model) {
  $.extend(this, model);
};

scout.LookupRow.prototype.setKey = function(key) {
  this.key = key;
};

scout.LookupRow.prototype.setText = function(text) {
  this.text = text;
};
