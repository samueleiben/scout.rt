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
scout.fields = {

  /**
   * @param $parent used to determine which HTML document is used to create the new HTML element
   * @returns an INPUT element as used in Scout forms.
   */
  makeTextField: function($parent, cssClass) {
    return $parent.makeElement('<input>', cssClass)
      .attr('type', 'text')
      .attr('autocomplete', 'NoAutocomplete') /* off and false are currently ignored in Chrome */
      .disableSpellcheck();
  },

  appendIcon: function($field, cssClass) {
    /*
     * Note: the field usually does $field.focus() when the icon is clicked.
     * Unfocusable is required because when the icon is clicked the browser is in the middle of setting
     * a new focus target, so we cannot simply change the focus target, because at the end the wrong target would be
     * focused and the popup would be closed immediately. With 'unfocusable' the focus manager will prevent the default focus (and does not execute _handleIEEvent either)
     */
    var $icon = $field.appendSpan('icon unfocusable');
    if (cssClass) {
      $icon.addClass(cssClass);
    }
    return $icon;
  },

  initTouch: function(field, model) {
    field.embedded = scout.nvl(model.embedded, false);
    // when 'touch' is not set explicitly, check the device
    field.touch = scout.nvl(model.touch, scout.device.supportsTouch());
  },

  /**
   * Calls JQuery $.text() for touch-devices and $.val() for all other devices, used together with #makeInputOrDiv().
   * Works as setter when called with 3 arguments, works a getter when called with 2 arguments.
   *
   * @return when called with 2 arguments: $field.text() or $field.val()
   */
  valOrText: function(field, $field, text) {
    if (arguments.length === 3) {
      if (field.touch) {
        $field.text(text);
      } else {
        $field.val(text);
      }
    } else {
      return field.touch ? $field.text() : $field.val();
    }
  },

  /**
   * Creates a DIV element for touch-devices and an INPUT element for all other devices.
   */
  makeInputOrDiv: function(field, cssClass) {
    if (field.touch) {
      return this.makeInputDiv(field.$container, cssClass);
    } else {
      return scout.fields.makeTextField(field.$container, cssClass);
    }
  },

  makeInputDiv: function($parent, cssClass) {
    return $parent.makeDiv(scout.strings.join(' ', 'input-field', cssClass));
  },

  // note: the INPUT element does not process the click event when the field is disabled
  // however, the DIV element used in touch-mode does process the event anyway, that's
  // why this check is required.
  handleOnClick: function(field) {
    return field.enabledComputed && !field.embedded && !field.popup;
  }

};