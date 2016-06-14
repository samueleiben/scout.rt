/*******************************************************************************
 * Copyright (c) 2010-2015 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.svg.client.svgfield;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.EventObject;

import org.apache.batik.dom.svg.SVGOMPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.svg.SVGPoint;

public class SvgFieldEvent extends EventObject {
  private static final Logger LOG = LoggerFactory.getLogger(SvgFieldEvent.class);
  private static final long serialVersionUID = 1L;

  private final int m_type;
  private final Point m_point;
  private final String m_appLinkRef;

  /**
   * A hyperlink was activated
   */
  public static final int TYPE_HYPERLINK = 10;
  /**
   * A element was clicked
   */
  public static final int TYPE_CLICKED = 20;

  SvgFieldEvent(ISvgField source, int type, SVGPoint point, String appLinkRef) {
    super(source);
    m_type = type;
    if (point != null) {
      //Wrap in a custom point because SVGPoint is not serializable but EventObject is
      m_point = new Point(point.getX(), point.getY());
    }
    else {
      m_point = null;
    }
    m_appLinkRef = appLinkRef;
  }

  public int getType() {
    return m_type;
  }

  public ISvgField getSvgField() {
    return (ISvgField) getSource();
  }

  public SVGPoint getPoint() {
    if (m_point == null) {
      return null;
    }

    return new SVGOMPoint(m_point.getX(), m_point.getY());
  }

  public String getAppLinkRef() {
    return m_appLinkRef;
  }

  /**
   * @deprecated use {@link #getAppLinkRef()} instead, will be removed in Scout 6.1.
   */
  @Deprecated
  public URL getURL() {
    String ref = getAppLinkRef();
    if (ref != null) {
      try {
        return new URL(ref);
      }
      catch (MalformedURLException e) {
        LOG.error("Malformed URL '{}'", ref, e);
      }
    }
    return null;
  }

  private class Point implements Serializable {
    private static final long serialVersionUID = 1L;

    private float m_x;
    private float m_y;

    public Point(float x, float y) {
      m_x = x;
      m_y = y;
    }

    public float getX() {
      return m_x;
    }

    public float getY() {
      return m_y;
    }
  }
}
