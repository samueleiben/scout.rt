/*
 * Copyright (c) 2010, 2023 BSI Business Systems Integration AG
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
import $ from 'jquery';
import {DoEntity, objects, scout, UiNotificationDo, UiNotificationPoller} from '../index';

export type UiNotificationHandler = (notification: DoEntity) => void;

let systems = new Map<string, System>();

export const uiNotifications = {
  systems: new Map,

  subscribe(topic: string, handler?: UiNotificationHandler, system?: string) {
    scout.assertParameter('topic', topic);
    scout.assertParameter('handler', handler);
    let systemObj = getOrInitSystem(system);
    let handlers = objects.getOrSetIfAbsent(systemObj.subscriptions, topic, () => new Set<UiNotificationHandler>());
    if (handlers.has(handler)) {
      // Already registered
      return;
    }
    handlers.add(handler);
    updatePoller(systemObj);
  },

  one(topic: string, handler?: UiNotificationHandler, system?: string) {
    let offFunc = (event: DoEntity) => {
      this.unsubscribe(topic, offFunc, system);
      handler(event);
    };
    this.subscribe(topic, offFunc, system);
  },

  when(topic: string, system?: string): JQuery.Promise<DoEntity> {
    let deferred = $.Deferred();
    this.one(topic, deferred.resolve.bind(deferred), system);
    return deferred.promise();
  },

  unsubscribe(topic: string, handler?: UiNotificationHandler, system?: string) {
    scout.assertParameter('topic', topic);
    let systemObj = getOrInitSystem(system);
    let subscriptions = systemObj.subscriptions;
    let handlers = subscriptions.get(topic) || new Set<UiNotificationHandler>();
    if (handler) {
      handlers.delete(handler);
    } else {
      handlers.clear();
    }
    if (handlers.size === 0) {
      subscriptions.delete(topic);
    }
    updatePoller(systemObj);
  },

  get pollers(): Map<string, UiNotificationPoller> {
    return new Map<string, UiNotificationPoller>(Array.from(systems.entries())
      .filter(([, system]) => !!system.poller)
      .map(([name, system]) => [name, system.poller]));
  },

  registerSystem(name: string, url: string) {
    if (systems.has(name)) {
      throw new Error(`System ${name} is already registered.`);
    }
    systems.set(name, new System(url));
  },

  unregisterSystem(name: string) {
    let system = systems.get(name);
    if (!system) {
      return;
    }
    system.poller?.stop();
    systems.delete(name);
  },

  tearDown() {
    for (const poller of Object.values(this.pollers) as UiNotificationPoller[]) {
      poller.stop();
    }
    systems.clear();
  }
};

function getOrInitSystem(system: string): System {
  if (!system && !systems.has('main')) {
    uiNotifications.registerSystem('main', 'api/ui-notifications');
  }

  let systemObj = systems.get(system || 'main');
  if (!systemObj) {
    throw new Error(`Unknown system ${system}`);
  }
  return systemObj;
}

function updatePoller(system: System) {
  let topics = Array.from(system.subscriptions.keys());
  let poller = system.poller;
  if (topics.length > 0 && !poller) {
    // First topic added -> create poller
    poller = scout.create(UiNotificationPoller, {
      url: system.url,
      dispatcher: system.dispatch.bind(system)
    });
    system.poller = poller;
  }

  if (!poller) {
    // No topics registered and no poller started -> do nothing
    return;
  }

  // Update poller with new topics
  poller.setTopics(topics);
  if (poller.topics.length === 0) {
    // Last topic removed -> stop poller
    poller.stop();
    system.poller = null;
  } else {
    poller.restart();
  }
}

class System {
  subscriptions: Map<string, Set<UiNotificationHandler>>;
  poller: UiNotificationPoller;
  url: string;

  constructor(url: string) {
    this.subscriptions = new Map<string, Set<UiNotificationHandler>>();
    this.url = url;
  }

  dispatch(notifications: UiNotificationDo[]) {
    for (const notification of notifications) {
      let handlers = this.subscriptions.get(notification.topic);
      if (!handlers) {
        $.log.isInfoEnabled() && $.log.info('Notification received but no subscribers registered');
        return;
      }
      for (const handler of handlers) {
        handler(notification.message);
      }
    }
  }
}
