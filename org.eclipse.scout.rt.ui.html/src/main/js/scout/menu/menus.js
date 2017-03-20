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
scout.menus = {

  /**
   * @memberOf scout.menus
   */
  CLOSING_EVENTS: 'mousedown.contextMenu keydown.contextMenu', //FIXME cgu: keydown/keyup is a bad idea -> interferes with ctrl click on table to multi select rows

  filterAccordingToSelection: function(prefix, selectionLength, menus, destination, onlyVisible, enableDisableKeyStroke, notAllowedTypes) {
    var allowedTypes = [];

    if (destination === scout.MenuDestinations.MENU_BAR) {
      allowedTypes = [prefix + '.EmptySpace', prefix + '.SingleSelection', prefix + '.MultiSelection'];
    } else if (destination === scout.MenuDestinations.CONTEXT_MENU) {
      allowedTypes = [prefix + '.SingleSelection', prefix + '.MultiSelection'];
    } else if (destination === scout.MenuDestinations.HEADER) {
      allowedTypes = [prefix + '.Header'];
    }

    if (allowedTypes.indexOf(prefix + '.SingleSelection') > -1 && selectionLength !== 1) {
      scout.arrays.remove(allowedTypes, prefix + '.SingleSelection');
    }
    if (allowedTypes.indexOf(prefix + '.MultiSelection') > -1 && selectionLength <= 1) {
      scout.arrays.remove(allowedTypes, prefix + '.MultiSelection');
    }
    notAllowedTypes = scout.arrays.ensure(notAllowedTypes);
    var fixedNotAllowedTypes = [];
    //ensure prefix
    prefix = prefix + '.';
    notAllowedTypes.forEach(function(type) {
      if (type.slice(0, prefix.length) !== prefix) {
        type = prefix + type;
      }
      fixedNotAllowedTypes.push(type);
    }, this);
    return scout.menus.filter(menus, allowedTypes, onlyVisible, enableDisableKeyStroke, fixedNotAllowedTypes);
  },

  /**
   * Filters menus that don't match the given types, or in other words: only menus with the given types are returned
   * from this method. The visible state is only checked if the parameter onlyVisible is set to true. Otherwise invisible items are returned and added to the
   * menu-bar DOM (invisible, however). They may change their visible state later. If there are any types in notAllowedTypes each menu is checked also against
   * these types and if they are matching the menu is filtered.
   */
  filter: function(menus, types, onlyVisible, enableDisableKeyStrokes, notAllowedTypes) {
    if (!menus) {
      return;
    }
    types = scout.arrays.ensure(types);
    notAllowedTypes = scout.arrays.ensure(notAllowedTypes);

    var filteredMenus = [],
      separatorCount = 0;

    menus.forEach(function(menu) {
      var childMenus = menu.childActions;
      if (childMenus.length > 0) {
        childMenus = scout.menus.filter(childMenus, types, onlyVisible, enableDisableKeyStrokes, notAllowedTypes);
        if (childMenus.length === 0) {
          scout.menus._enableDisableMenuKeyStroke(menu, enableDisableKeyStrokes, true);
          return;
        }
      } // Don't check the menu type for a group
      else if (!scout.menus._checkType(menu, types) || (notAllowedTypes.length !== 0 && scout.menus._checkType(menu, notAllowedTypes))) {
        scout.menus._enableDisableMenuKeyStroke(menu, enableDisableKeyStrokes, true);
        return;
      }

      if (onlyVisible && !menu.visible) {
        scout.menus._enableDisableMenuKeyStroke(menu, enableDisableKeyStrokes, true);
        return;
      }
      if (menu.separator) {
        separatorCount++;
      }
      scout.menus._enableDisableMenuKeyStroke(menu, enableDisableKeyStrokes, false);
      filteredMenus.push(menu);
    });

    // Ignore menus with only separators
    if (separatorCount === filteredMenus.length) {
      return [];
    }
    return filteredMenus;
  },

  checkType: function(menu, types) {
    types = scout.arrays.ensure(types);
    if (menu.childActions.length > 0) {
      var childMenus = scout.menus.filter(menu.childActions, types);
      return (childMenus.length > 0);
    }
    return scout.menus._checkType(menu, types);
  },

  _enableDisableMenuKeyStroke: function(menu, activated, exclude) {
    if (activated) {
      menu.excludedByFilter = exclude;
    }
  },

  /**
   * Checks the type of a menu. Don't use this for menu groups.
   */
  _checkType: function(menu, types) {
    if (!types || types.length === 0) {
      return false;
    }
    if (!menu.menuTypes) {
      return false;
    }
    for (var j = 0; j < types.length; j++) {
      if (menu.menuTypes.indexOf(types[j]) > -1) {
        return true;
      }
    }
  },

  showContextMenuWithWait: function(session, func) {
    var argumentsArray = Array.prototype.slice.call(arguments);
    argumentsArray.shift(); // remove argument session
    argumentsArray.shift(); // remove argument func, remainder: all other arguments

    if (session.areRequestsPending() || session.areEventsQueued()) {
      session.listen().done(onEventsProcessed);
    } else {
      func.apply(this, argumentsArray);
    }

    function onEventsProcessed() {
      func.apply(this, argumentsArray);
    }
  },

  createEllipsisMenu: function(options) {
    var defaults = {
      iconId: scout.icons.ELLIPSIS_V,
      tabbable: false
    };
    options = $.extend({}, defaults, options);
    return scout.create('Menu', options);
  },

  moveMenuIntoEllipsis: function(menu, ellipsis) {
    menu.remove();
    menu.overflow = true;
    menu.overflowMenu = ellipsis;
    ellipsis.childActions.unshift(menu); // add as first element
  },

  removeMenuFromEllipsis: function(menu, $parent) {
    menu.overflow = false;
    menu.overflowMenu = null;
    if (!menu.rendered) {
      menu.render($parent);
    }
  },

  updateTabbableMenu: function(menus, preferredMenus) {
    preferredMenus = scout.arrays.ensure(preferredMenus);

    // Find the current tabbable menu (and remove the marker)
    var currentTabbableMenu = null;
    menus.forEach(function(menu) {
      if (!currentTabbableMenu && menu.tabbable) {
        currentTabbableMenu = menu;
      }
      menu.setTabbable(false);
    });

    // Mark one of the menus as tabbable, so it can be focused by keystroke.
    // - First choice is the currently marked menu
    // - Second choice is one of the "preferred menus" passed to this function
    // - Otherwise, use the first of all menus that is a tab target
    var tabbableMenu = isTabTargetFunc(currentTabbableMenu) ? currentTabbableMenu : null;
    if (!tabbableMenu) {
      tabbableMenu = scout.arrays.find(preferredMenus, isTabTargetFunc);
    }
    if (!tabbableMenu) {
      tabbableMenu = scout.arrays.find(menus, isTabTargetFunc);
    }

    // Mark it
    if (tabbableMenu) {
      tabbableMenu.setTabbable(true);
    }

    // ----- Helper functions -----

    function isTabTargetFunc(menu) {
      return menu && menu.isTabTarget();
    }
  }
};

scout.MenuDestinations = {
  MENU_BAR: 1,
  CONTEXT_MENU: 2,
  HEADER: 3
};
