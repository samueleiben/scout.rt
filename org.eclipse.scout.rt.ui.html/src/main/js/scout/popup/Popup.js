/**
 * Options:
 * <ul>
 * <li><b>initialFocus</b> a Function that returns the element to be focused or a <code>scout.FocusRule</code>. Default returns <code>scout.FocusRule.AUTO</code></li>
 * <li><b>focusableContainer</b> a Boolean whether or not the container of the Popup is focusable</li>
 * </ul>
 *
 */
scout.Popup = function(session, options) {
  // FIXME AWE: use this.options property here
  scout.Popup.parent.call(this);
  this.init(session);

  options = options || {};
  this._mouseDownHandler = this._onMouseDown.bind(this);
  this._scrollHandler = this._onAnchorScroll.bind(this);
  this._popupOpenHandler = this._onPopupOpen.bind(this);

  this.keyStrokeAdapter = this._createKeyStrokeAdapter();
  this.openEvent;
  this.anchorBounds = options.anchorBounds;
  if (options.location) {
    this.anchorBounds = new scout.Rectangle(options.location.x, options.location.y, 0, 0);
  }
  this.$anchor = options.$anchor;
  this.windowPaddingX = scout.helpers.nvl(options.windowPaddingX, 10);
  this.windowPaddingY = scout.helpers.nvl(options.windowPaddingY, 5);
  this.installFocusContext = scout.helpers.nvl(options.installFocusContext, true);
  this.initialFocus = scout.helpers.nvl(options.initialFocus, function() { return scout.FocusRule.AUTO; });
  this.focusableContainer = scout.helpers.nvl(options.focusableContainer, false);
  this._addEventSupport();

};
scout.inherits(scout.Popup, scout.Widget);

scout.Popup.prototype.render = function($parent, event) {
  scout.Popup.parent.prototype.render.call(this, $parent);
  this.openEvent = event;
  this._attachCloseHandler();
};

scout.Popup.prototype._postRender = function() {
  // position must be set _before_ focus is installed, otherwise we'd focus an element
  // that is currently not on the screen. Which would cause the whole desktop to
  // be shifted for a few pixels.
  this.position();
  if (this.installFocusContext) {
    this.$container.installFocusContext(this.session, this.initialFocus());
  }
};

scout.Popup.prototype.remove = function() {
  if (!this.rendered) {
    return;
  }

  if (this.installFocusContext) {
    this.$container.uninstallFocusContext(this.session);
  }
  scout.Popup.parent.prototype.remove.call(this);

  // remove all clean-up handlers
  this._detachCloseHandler();
  this.rendered = false;
};

scout.Popup.prototype._render = function($parent) {
  if (!$parent) {
    $parent = this.session.$entryPoint;
  }
  this.$container = $.makeDiv('popup').appendTo($parent);

  // Add programmatic 'tabindex' if the $container itself should be focusable (used by context menu popups)
  if (this.installFocusContext && this.focusableContainer) {
    this.$container.attr('tabindex', -1);
  }
};

scout.Popup.prototype.close = function(event) {
  if ((event && this.openEvent && event.originalEvent !== this.openEvent.originalEvent) || !event || !this.openEvent) {
    this._trigger('close', event);
    this.remove();
  }
};

/**
 * Install listeners to close the popup once clicking outside the popup,
 * or changing the anchor's scroll position, or another popup is opened.
 */
scout.Popup.prototype._attachCloseHandler = function() {
  // Fire event that this popup is opening
  this.session.desktop._trigger('popupopen', {popup: this});

  // Register listeners to close the popup.
  $(document).on('mousedown', this._mouseDownHandler);
  this.session.desktop.on('popupopen', this._popupOpenHandler);

  if (this.$anchor) {
    scout.scrollbars.onScroll(this.$anchor, this._scrollHandler);
  }
};

scout.Popup.prototype._detachCloseHandler = function() {
  if (this.$anchor) {
    scout.scrollbars.offScroll(this._scrollHandler);
  }
  this.session.desktop.off('popupopen', this._popupOpenHandler);
    $(document).off('mousedown', this._mouseDownHandler);
};

scout.Popup.prototype._onMouseDown = function(event) {
  var $target = $(event.target);
  // close the popup only if the click happened outside of the popup
  if (this.$container && this.$container.has($target).length === 0) {
    this._onMouseDownOutside(event);
  }
};

/**
 * Method invoked once a mouse down event occurs outside the popup.
 */
scout.Popup.prototype._onMouseDownOutside = function(event) {
  this.close(event);
};

/**
 * Method invoked once the 'options.$anchor' is scrolled.
 */
scout.Popup.prototype._onAnchorScroll = function(event) {
  this.close(event);
};

/**
 * Method invoked once a popup is opened.
 */
scout.Popup.prototype._onPopupOpen = function(event) {
  if (event.popup !== this) {
    this.close(event);
  }
};

scout.Popup.prototype.prefLocation = function($container, openingDirectionY) {
  var x, y, anchorBounds, height;
  if (!this.anchorBounds && !this.$anchor) {
    return;
  }
  openingDirectionY = openingDirectionY || 'down';
  $container.removeClass('up down');
  $container.addClass(openingDirectionY);
  height = $container.outerHeight(true),

  anchorBounds = this.anchorBounds;
  if (!anchorBounds) {
    anchorBounds = this.$anchor && scout.graphics.offsetBounds(this.$anchor);
  }

  x = anchorBounds.x;
  y = anchorBounds.y;
  if (openingDirectionY === 'up') {
    y -= height;
  } else if (openingDirectionY === 'down') {
    y += anchorBounds.height;
  }

  // FIXME AWE/CGU: for smartfield-popups we must adjust the popup-size
  // so the popup fits into the available space. It's simple to calculate
  // the correct size, but it's not so easy to resize the popup-container
  // since this method here is called in the middle of a layout-operation
  // of the SmartfieldPopupLayout. When we resize the contaienr here it
  // will result in an endless loop. We should:
  // 1. calc the pref. size of the popup
  // 2. find the right position for the popup (up, down)
  // 3. check if the popup size is small enough to be completely visible
  //    in the current window
  // 4. if required reduce the popup-size, so it will fit into the window
  // 5. finally set the size and layout the popup and its children.
  //
  // To avoid that the popup switches between up and down position while
  // typing, we could always find the position by using the max. popup
  // size instead by checking against the current size.
  return {
    x: x,
    y: y
  };
};

scout.Popup.prototype.overlap = function($container, location) {
  if (!$container || !location) {
    return;
  }
  var overlapX, overlapY,
    height = $container.outerHeight(),
    width = $container.outerWidth(),
    left = location.x,
    top = location.y;

  overlapX = left + width + this.windowPaddingX - $(window).width();
  overlapY = top + height + this.windowPaddingY - $(window).height();
  return {
    x: overlapX,
    y: overlapY
  };
};

scout.Popup.prototype.adjustLocation = function($container, location, anchorBounds) {
  var openingDirection, left, top,
    overlap = this.overlap($container, location);

  if (overlap.y > 0) {
    // switch opening direction
    openingDirection = 'up';
    location = this.prefLocation($container, openingDirection);
  }
  left = location.x,
  top = location.y;
  if (overlap.x > 0) {
    // Move popup to the left until it gets fully visible
    left -= overlap.x;
  }
  return {
    x: left,
    y: top
  };
};

scout.Popup.prototype.position = function() {
  this._position(this.$container);
};

scout.Popup.prototype._position = function($container) {
  var location = this.prefLocation($container);
  if (!location) {
    return;
  }
  location = this.adjustLocation($container, location);
  this.setLocation(location);
};

scout.Popup.prototype.setLocation = function(location) {
  this.$container
    .css('left', location.x)
    .css('top', location.y);
};

scout.Popup.prototype._createKeyStrokeAdapter = function() {
  return new scout.PopupKeyStrokeAdapter(this);
};
