/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.asgardio.tomcat.oidc.agent;

import io.asgardio.java.oidc.sdk.bean.TokenData;
import io.asgardio.java.oidc.sdk.util.CommonUtils;
import net.minidev.json.JSONObject;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Optional;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A servlet class to handle OIDC metadata.
 */
public class MetadataServlet extends HttpServlet {

    private static void sendNotFound(final HttpServletResponse response) throws IOException {

        response.sendError(404);
    }

    private static void metadataResponse(final Cookie appIdCookie, final HttpServletResponse response)
            throws IOException {

        final Optional<TokenData> tokenData = CommonUtils.getTokenDataByCookieID(appIdCookie.getValue());

        if (tokenData.isPresent()) {
            final JSONObject jsonObject = new JSONObject();
            jsonObject.put("AccessToken", tokenData.get().getAccessToken());
            jsonObject.put("ApiEndpoint", SSOAgentContextEventListener.getPropertyByKey("api_endpoint"));

            final PrintWriter responseWriter = response.getWriter();
            responseWriter.write(jsonObject.toString());

            response.setHeader("Content-Type", "application/json");

            responseWriter.close();

        } else {
            sendNotFound(response);
        }
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
            throws IOException {

        final Optional<Cookie> appIdCookie = CommonUtils.getAppIdCookie(req);

        if (appIdCookie.isPresent()) {
            metadataResponse(appIdCookie.get(), resp);
        } else {
            sendNotFound(resp);
        }
    }

}
