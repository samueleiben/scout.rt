/*******************************************************************************
 * Copyright (c) 2010 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.ui.html.json.testing;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.scout.rt.ui.html.json.IJsonSession;
import org.eclipse.scout.rt.ui.html.json.JsonEvent;
import org.eclipse.scout.rt.ui.html.json.JsonException;
import org.eclipse.scout.rt.ui.html.json.JsonRequest;
import org.eclipse.scout.rt.ui.html.json.JsonResponse;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.mockito.Mockito;

public class JsonTestUtility {

  public static IJsonSession createAndInitializeJsonSession() {
    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    Mockito.when(request.getLocale()).thenReturn(new Locale("de_CH"));
    Mockito.when(request.getHeader("User-Agent")).thenReturn("dummy");
    JSONObject jsonReqObj = new JSONObject();
    try {
      jsonReqObj.put(JsonRequest.PROP_JSON_SESSION_ID, "1.1");
    }
    catch (JSONException e) {
      throw new JsonException(e);
    }
    JsonRequest jsonRequest = new JsonRequest(jsonReqObj);
    IJsonSession jsonSession = new TestEnvironmentJsonSession();
    jsonSession.init(request, jsonRequest);
    return jsonSession;
  }

  // FIXME AWE: (event) check callers
  public static JsonEvent createJsonEvent(String type) throws JSONException {
    return new JsonEvent(type, null, new JSONObject());
  }

  public static List<JsonEvent> extractEventsFromResponse(JsonResponse response, String eventType) throws JSONException {
    List<JsonEvent> list = new ArrayList<>();
    for (JsonEvent event : response.getEventList()) {
      if (event.getType().equals(eventType)) {
        list.add(event);
      }
    }
    return list;
  }

  public static void assertGC(WeakReference<?> ref) {
    int maxRuns = 50;
    for (int i = 0; i < maxRuns; i++) {
      if (ref.get() == null) {
        return;
      }

      System.gc();
      try {
        Thread.sleep(50);
      }
      catch (InterruptedException e) {
      }
    }
    Assert.fail("Potential memory leak, object " + ref.get() + "still exists after gc");
  }

  public static JSONObject getAdapterData(JSONObject json, String id) throws JSONException {
    return json.getJSONObject("adapterData").getJSONObject(id);
  }

  public static JSONObject getEvent(JSONObject json, int index) throws JSONException {
    return (JSONObject) json.getJSONArray("events").get(index);
  }

  public static JSONObject getPropertyChange(JSONObject json, int index) throws JSONException {
    return getEvent(json, index).getJSONObject("properties");
  }

}
