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
scout.Image = function() {
  scout.Image.parent.call(this);
  this._addEventSupport();

  this.cachedDimension = null;
};
scout.inherits(scout.Image, scout.Widget);

scout.Image.prototype._init = function(options) {
  scout.Image.parent.prototype._init.call(this, options);
  this.prepend = options.prepend;
  this.imageUrl = options.imageUrl;
  this.cssClass = options.cssClass;
};

scout.Image.prototype._render = function($parent) {
  this.$container = $parent.makeElement('<img>', 'image')
    .on('load', this._onImageLoad.bind(this))
    .on('error', this._onImageError.bind(this));

  if (scout.nvl(this.prepend, true)) {
    this.$container.prependTo($parent);
  } else {
    this.$container.appendTo($parent);
  }

  this.htmlComp = new scout.HtmlComponent(this.$container, this.session);
};

scout.Image.prototype._renderProperties = function() {
  scout.Image.parent.prototype._renderProperties.call(this);
  this._renderCssClass();
  this._renderImageUrl();
};

scout.Image.prototype.setCssClass = function(cssClass) {
  if (this.rendered) {
    this._removeCssClass();
  }
  this.cssClass = cssClass;
  if (this.rendered) {
    this._renderCssClass();
  }
};

scout.Image.prototype._removeCssClass = function() {
  this.$container.removeClass(this.cssClass);
};

scout.Image.prototype._renderCssClass = function() {
  this.$container.addClass(this.cssClass);
};

scout.Image.prototype.setImageUrl = function(imageUrl) {
  if (imageUrl !== this.imageUrl) {
    this.cachedDimension = null;
  }
  this.imageUrl = imageUrl;
  if (this.rendered) {
    this._renderImageUrl();
  }
};

scout.Image.prototype._renderImageUrl = function() {
  this.$container.attr('src', this.imageUrl);
  // Hide <img> when it has no content (event 'load' will not fire)
  if (!this.imageUrl) {
    this.$container.addClass('empty');
    this.$container.removeClass('broken');
  }

  if (this.cachedDimension && !this.autoFit) {
    this.$container
      .cssWidth(this.cachedDimension.width)
      .cssHeight(this.cachedDimension.height);
  }
};

scout.Image.prototype.setAutoFit = function(autoFit) {
  this.autoFit = autoFit;
  if (this.rendered) {
    this._renderAutoFit();
  }
};

scout.Image.prototype._renderAutoFit = function() {
  this.$container.toggleClass('autofit', this.autoFit);
  if (this.autoFit) {
    this.$container.css('width', null).css('height', null);
  }
};

scout.Image.prototype._onImageLoad = function(event) {
  if (this.rendered) {
    this.$container.removeClass('empty broken');
    this.cachedDimension = (this.autoFit ? null : scout.graphics.getSize(this.$container, true));
    this.invalidateLayoutTree();
    this.trigger('load');
  }
};

scout.Image.prototype._onImageError = function(event) {
  if (this.rendered) {
    this.$container.addClass('empty broken');
    this.cachedDimension = null;
    this.invalidateLayoutTree();
    this.trigger('error');
  }
};
