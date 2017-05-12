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
/**
 * ValueField assumes $field has a .val() method which returns the value of that field.
 * @abstract
 */
scout.ValueField = function() {
  scout.ValueField.parent.call(this);
  this.displayText = null;
  this.initialValue = null;
  this.value = null;
};
scout.inherits(scout.ValueField, scout.FormField);

scout.ValueField.prototype._init = function(model) {
  scout.ValueField.parent.prototype._init.call(this, model);
  this._setValue(this.value);
};

scout.ValueField.prototype._renderProperties = function() {
  scout.ValueField.parent.prototype._renderProperties.call(this);
  this._renderDisplayText();
};

scout.ValueField.prototype._renderDisplayText = function() {
  this.$field.val(this.displayText);
};

scout.ValueField.prototype._readDisplayText = function() {
  return scout.fields.valOrText(this, this.$field);
};

scout.ValueField.prototype._onFieldBlur = function() {
  this.acceptInput(false);
};

/**
 * Accepts the current input and writes it to the model.
 * <p>
 * This method is typically called by the _onBlur() function of the field, but may actually be called from anywhere (e.g. button, actions, cell editor, etc).
 * It is also called by the _aboutToBlurByMouseDown() function, which is required because our Ok- and Cancel-buttons are not focusable (thus _onBlur() is
 * never called) but changes in the value-field must be sent to the server anyway when a button is clicked.
 * <p>
 * The default reads the display text using this._readDisplayText() and writes it to the model by calling _triggerDisplayTextChanged().
 * If subclasses don't have a display-text or want to write another state to the server, they may override this method.
 */
scout.ValueField.prototype.acceptInput = function(whileTyping) {
  whileTyping = !!whileTyping; // cast to boolean
  var displayText = scout.nvl(this._readDisplayText(), '');

  // trigger only if displayText has really changed
  if (this._checkDisplayTextChanged(displayText, whileTyping)) {
    // Don't call setDisplayText() to prevent re-rendering of display text (which is unnecessary and
    // might change the cursor position). Don't call _callSetProperty() as well, as this eventually
    // executes this._setDisplayText(), which updates the value.
    this._setProperty('displayText', displayText);
    if (!whileTyping) {
      this.parseAndSetValue(displayText);
    }
    this._triggerDisplayTextChanged(displayText, whileTyping);
  }
};

scout.ValueField.prototype.parseAndSetValue = function(displayText) {
  var parsedValue = this._parseValue(displayText);
  this.setValue(parsedValue);
};

scout.ValueField.prototype._parseValue = function(displayText) {
  // TODO [7.0] awe: this impl. is far too simple. Check how it is done in Java Scout, also discuss with A.SA
  // TODO [7.0] awe: check what happens when server does execFormatValue: abc -> ABC. Could this possibly
  //                  lead to cycles? Probably it's not possible because our filters and because the adapter
  //                  does not yet know the value. Example from BSH: collator AE - Ä
  return displayText;
};

scout.ValueField.prototype._checkDisplayTextChanged = function(displayText, whileTyping) {
  var oldDisplayText = scout.nvl(this.displayText, '');
  return displayText !== oldDisplayText;
};

/**
 * Method invoked upon a mousedown click with this field as the currently focused control, and is invoked just before the mousedown click will be interpreted.
 * However, the mousedown target must not be this control, but any other control instead.
 *
 * The default implementation checks, whether the click occurred outside this control, and if so invokes 'ValueField.acceptInput'.
 *
 * @param target
 *        the DOM target where the mouse down event occurred.
 */
scout.ValueField.prototype.aboutToBlurByMouseDown = function(target) {
  var eventOnField = this.$field.isOrHas(target);

  if (!eventOnField) {
    this.acceptInput(); // event outside this value field.
  }
};

scout.ValueField.prototype._triggerDisplayTextChanged = function(displayText, whileTyping) {
  var event = {
    displayText: displayText,
    whileTyping: !!whileTyping
  };
  this.trigger('displayTextChanged', event);
};

scout.ValueField.prototype.setDisplayText = function(displayText) {
  this.setProperty('displayText', displayText);
};

// TODO [7.0] awe: check fields like DateField where setTimestamp is used instead of setValue
scout.ValueField.prototype.setValue = function(value) {
  this.setProperty('value', value);
};

scout.ValueField.prototype._setValue = function(value) {
  var oldValue = this.value;
  this.value = this._validateValue(value);
  this._updateTouched();
  this._updateEmpty();
  this.triggerPropertyChange('value', oldValue, this.value);

  // If a displayText is provided initially, use that text instead of using formatValue to generate a text based on the value
  if (this.initialized || scout.objects.isNullOrUndefined(this.displayText)) {
    this._updateDisplayText();
  }
};

scout.ValueField.prototype._updateDisplayText = function() {
  var returned = this._formatValue(this.value);
  if (returned && $.isFunction(returned.promise)) {
    // Promise is returned -> set display text later
    returned
      .done(this.setDisplayText.bind(this))
      .fail(function() {
        this.setDisplayText('');
        $.log.error('Could not resolve display text.');
      }.bind(this));
  } else {
    this.setDisplayText(returned);
  }
};

/**
 * @returns the validated value
 */
scout.ValueField.prototype._validateValue = function(value) {
  return value;
};

/**
 * @returns the formatted string or a promise
 */
scout.ValueField.prototype._formatValue = function(value) {
  return scout.nvl(value, ''); // [6.2] awe: check impl. for fields other than StringField
};

scout.ValueField.prototype._updateTouched = function() {
  this.touched = !scout.objects.equals(this.value, this.initialValue);
};

scout.ValueField.prototype.addField = function($field) {
  scout.ValueField.parent.prototype.addField.call(this, $field);
  this.$field.data('valuefield', this);
};

scout.ValueField.prototype._onStatusMousedown = function(event) {
  if (this.menus && this.menus.length > 0) {
    var $activeElement = this.$container.activeElement();
    if ($activeElement.data('valuefield') === this ||
      $activeElement.parent().data('valuefield') === this) {
      this.acceptInput();
    }
  }

  scout.ValueField.parent.prototype._onStatusMousedown.call(this, event);
};

scout.ValueField.prototype._getCurrentMenus = function() {
  if (this.currentMenuTypes) {
    var menuTypes = this.currentMenuTypes.map(function(elem) {
      return 'ValueField.' + elem;
    });
    return scout.menus.filter(this.menus, menuTypes);
  }
  return scout.ValueField.parent.prototype._getCurrentMenus.call(this);
};

scout.ValueField.prototype._renderCurrentMenuTypes = function() {
  // If a tooltip is shown, update it with the new menus
  if (this.tooltip) {
    this._showStatusMessage();
  }
};

// ==== static helper methods ==== //

/**
 * Invokes 'ValueField.aboutToBlurByMouseDown' on the currently active value field.
 * This method has no effect if another element is the focus owner.
 */
scout.ValueField.invokeValueFieldAboutToBlurByMouseDown = function(target) {
  var activeValueField = this._getActiveValueField(target);
  if (activeValueField) {
    activeValueField.aboutToBlurByMouseDown(target);
  }
};

/**
 * Invokes 'ValueField.acceptInput' on the currently active value field.
 * This method has no effect if another element is the focus owner.
 */
scout.ValueField.invokeValueFieldAcceptInput = function(target) {
  var activeValueField = this._getActiveValueField(target);
  if (activeValueField) {
    activeValueField.acceptInput();
  }
};

/**
 * Returns the currently active value field, or null if another element is active.
 * Also, if no value field currently owns the focus, its parent is checked to be a value field and is returned accordingly.
 * That is used in DateField.js with multiple input elements.
 */
scout.ValueField._getActiveValueField = function(target) {
  var $activeElement = $(target.ownerDocument.activeElement),
    valueField = $activeElement.data('valuefield') || $activeElement.parent().data('valuefield');
  return valueField && !(valueField.$field && valueField.$field.hasClass('disabled')) ? valueField : null;
};

scout.ValueField.prototype.markAsSaved = function() {
  scout.ValueField.parent.prototype.markAsSaved.call(this);
  this.initialValue = this.value;
};

/**
 * @override
 */
scout.ValueField.prototype._updateEmpty = function() {
  this.empty = this.value === null || this.value === undefined;
};
