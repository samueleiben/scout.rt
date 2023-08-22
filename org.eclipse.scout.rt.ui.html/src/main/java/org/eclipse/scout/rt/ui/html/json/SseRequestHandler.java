/*
 * Copyright (c) 2010, 2023 BSI Business Systems Integration AG
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.scout.rt.ui.html.json;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.eclipse.scout.rt.platform.BEANS;
import org.eclipse.scout.rt.platform.util.StringUtility;
import org.eclipse.scout.rt.shared.SharedConfigProperties.ServiceTunnelTargetUrlProperty;
import org.eclipse.scout.rt.shared.servicetunnel.http.DefaultAuthToken;
import org.eclipse.scout.rt.shared.servicetunnel.http.DefaultAuthTokenSigner;
import org.eclipse.scout.rt.shared.servicetunnel.http.HttpServiceTunnel;
import org.eclipse.scout.rt.ui.html.AbstractUiServletRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO CGU CN only proxy? Pure Scout JS apps use ClientNotificationResource?
public class SseRequestHandler extends AbstractUiServletRequestHandler {
  private static final Logger LOG = LoggerFactory.getLogger(SseRequestHandler.class);

  protected boolean acceptRequest(HttpServletRequest req) {
    return StringUtility.startsWith(req.getPathInfo(), getLocalContextPathPrefix());
  }

  protected String getLocalContextPathPrefix() {
    return "/sse";
  }

  private Integer m_id = 0;

  @Override
  protected boolean handleGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    if (!acceptRequest(req)) {
      return false;
    }

    System.out.println("short: " + req.getParameter("short"));

//    resp.setStatus(HttpStatus.SC_ACCEPTED);
//    resp.setHeader("Access-Control-Allow-Credentials", "true");
//    resp.setContentType("text/event-stream");
//    resp.setCharacterEncoding("UTF-8");
//
//    PrintWriter writer = resp.getWriter();
//
//    for (int i = 0; i < 10; i++) {
//
//      writer.write("id: "+ System.currentTimeMillis() +"\n");
////      writer.write("event: notification\n");
//      writer.write("data: hi there " + i +"\n\n");
//      writer.flush();
//
//      try {
//        Thread.sleep(1000);
//      } catch (InterruptedException e) {
//        e.printStackTrace();
//      }
//    }
//    writer.close();
//

    String baseUrl = BEANS.get(ServiceTunnelTargetUrlProperty.class).getValue().replace("/process", "");

    HttpClient client = HttpClient.newBuilder()
        .version(Version.HTTP_1_1)
        .followRedirects(Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(20))
        .build();

    String url = baseUrl + "/api/notifications/sse";
    DefaultAuthToken token = BEANS.get(DefaultAuthTokenSigner.class).createDefaultSignedToken(DefaultAuthToken.class);
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .GET()
        .header("Accept", "text/event-stream")
        .header("X-Requested-With", "sse")
        .header(HttpServiceTunnel.TOKEN_AUTH_HTTP_HEADER, token.toString())
        .build();
    try {
      HttpResponse<InputStream> response = client.send(request, BodyHandlers.ofInputStream());
      resp.setContentType("text/event-stream");
      resp.setStatus(response.statusCode());
      resp.setCharacterEncoding("UTF-8");
      response.body().transferTo(resp.getOutputStream());
      // TODO CGU CN close input stream somehow?
    }
    catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    return true;
  }
}
