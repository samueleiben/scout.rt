/*
 * Copyright (c) 2010, 2023 BSI Business Systems Integration AG
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.scout.rt.api.data.uinotification;

import java.util.Collection;
import java.util.List;

import javax.annotation.Generated;

import org.eclipse.scout.rt.dataobject.DoEntity;
import org.eclipse.scout.rt.dataobject.DoList;

public class UiNotificationRequest extends DoEntity {

  public DoList<TopicDo> topics() {
    return doList("topics");
  }

  /* **************************************************************************
   * GENERATED CONVENIENCE METHODS
   * *************************************************************************/

  @Generated("DoConvenienceMethodsGenerator")
  public UiNotificationRequest withTopics(Collection<? extends TopicDo> topics) {
    topics().updateAll(topics);
    return this;
  }

  @Generated("DoConvenienceMethodsGenerator")
  public UiNotificationRequest withTopics(TopicDo... topics) {
    topics().updateAll(topics);
    return this;
  }

  @Generated("DoConvenienceMethodsGenerator")
  public List<TopicDo> getTopics() {
    return topics().get();
  }
}
