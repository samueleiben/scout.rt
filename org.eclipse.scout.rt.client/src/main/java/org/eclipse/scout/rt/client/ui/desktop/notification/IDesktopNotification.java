/*
 * Copyright (c) 2010-2018 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 */
package org.eclipse.scout.rt.client.ui.desktop.notification;

import java.util.concurrent.TimeUnit;

import org.eclipse.scout.rt.client.ui.notification.INotification;

/**
 * A notification is used to display a short information on the Desktop. If the given duration is >= 0, the notification
 * disappears automatically after the duration has passed.
 *
 * @since 5.2
 */
public interface IDesktopNotification extends INotification {

  /**
   * Default duration a notification is displayed is 5 seconds.
   */
  long DEFAULT_DURATION = TimeUnit.SECONDS.toMillis(5);

  /**
   * Duration is infinite which means notification is not automatically removed.
   */
  long INFINITE_DURATION = -1;

  /**
   * No native notification is shown.
   */
  String NATIVE_NOTIFICATION_VISIBILITY_NONE = "none";

  /**
   * The native notification is only shown if the application is in background.
   */
  String NATIVE_NOTIFICATION_VISIBILITY_BACKGROUND = "background";

  /**
   * The native notification is always shown.
   */
  String NATIVE_NOTIFICATION_VISIBILITY_ALWAYS = "always";

  DesktopNotification withDuration(long duration);

  /**
   * Duration in milliseconds while the notification is displayed.
   * <p>
   * A value <= 0 indicates an infinite duration, i.e. the notification is never closed automatically.
   */
  long getDuration();

  DesktopNotification withNativeOnly(boolean nativeOnly);

  /**
   * @return True, to only show the native desktop notification. False, to show both, the regular and the native
   *         notification. If and when a native desktop notification is shown is controlled by
   *         {@link #getNativeNotificationVisibility()}.
   */
  boolean isNativeOnly();

  DesktopNotification withNativeNotificationVisibility(String nativeNotificationVisibility);

  /**
   * see {@link #NATIVE_NOTIFICATION_VISIBILITY_NONE}, {@link #NATIVE_NOTIFICATION_VISIBILITY_ALWAYS} and
   * {@link #NATIVE_NOTIFICATION_VISIBILITY_BACKGROUND}
   */
  String getNativeNotificationVisibility();
}
