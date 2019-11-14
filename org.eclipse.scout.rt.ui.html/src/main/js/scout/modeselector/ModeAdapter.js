/*******************************************************************************
 * Copyright (c) 2014-2018 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
scout.ModeAdapter = function() {
  scout.ModeAdapter.parent.call(this);
};
scout.inherits(scout.ModeAdapter, scout.ActionAdapter);

scout.ModeAdapter.prototype._initModel = function(model, parent) {
  model = scout.ModeAdapter.parent.prototype._initModel.call(this, model, parent);
  model.inheritAccessibility = true;
  return model;
};