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

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.AuthorizationGrant;
import com.nimbusds.oauth2.sdk.TokenErrorResponse;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.id.ClientID;
import io.asgardio.java.oidc.sdk.bean.TokenData;
import io.asgardio.java.oidc.sdk.bean.User;
import io.asgardio.java.oidc.sdk.exception.SSOAgentClientException;
import io.asgardio.java.oidc.sdk.exception.SSOAgentServerException;
import io.asgardio.java.oidc.sdk.util.CommonUtils;
import io.asgardio.java.oidc.sdk.util.SSOAgentConstants;
import net.minidev.json.JSONObject;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import static io.asgardio.java.oidc.sdk.util.CommonUtils.getAppIdCookie;

/**
 * A servlet class to handle OIDC callback responses.
 */
public class OIDCCallbackResponseHandler extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        handleOIDCCallback(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        handleOIDCCallback(req, resp);
    }

    private void handleOIDCCallback(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException {

        Properties properties = SSOAgentContextEventListener.getProperties();
        String indexPage;

        // Error response from IDP
        if (isError(request)) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            handleAppIdCookieForLogout(request, response);
            indexPage = getIndexPage(session, properties);
            response.sendRedirect(indexPage);
            return;
        }

        // Create the initial session
        if (request.getSession(false) == null) {
            request.getSession(true);
        }
        indexPage = getIndexPage(request.getSession(false), properties);

        // Validate callback properties
        if (isLogout(request)) {
            CommonUtils.logout(request, response);
            response.sendRedirect(indexPage);
            return;
        }

        // Obtain and store session_state against this session
        request.getSession(false)
                .setAttribute(SSOAgentConstants.SESSION_STATE, request.getParameter(SSOAgentConstants.SESSION_STATE));

        if (isLogin(request)) {
            try {
                // Obtain token response
                getToken(request, response);
                response.sendRedirect("home.jsp");
            } catch (SSOAgentServerException | SSOAgentClientException e) {
                response.sendRedirect(indexPage);
            }
        }
    }

    private void getToken(final HttpServletRequest request, final HttpServletResponse response)
            throws SSOAgentServerException, SSOAgentClientException, IOException {

        HttpSession session = request.getSession(false);
        if (!checkOAuth(request)) {
            session.invalidate();
            session = request.getSession();
        }
        final Optional<Cookie> appIdCookie = getAppIdCookie(request);
        final Properties properties = SSOAgentContextEventListener.getProperties();
        final TokenData storedTokenData;

        if (appIdCookie.isPresent()) {
            storedTokenData = CommonUtils.TOKEN_STORE.get(appIdCookie.get().getValue());
            if (storedTokenData != null) {
                setTokenDataToSession(session, storedTokenData);
                return;
            }
        }

        final String authzCode = request.getParameter("code");

        if (authzCode == null) {
            throw new SSOAgentServerException("Authorization code not present in callback");
        }

        AuthorizationCode authorizationCode = new AuthorizationCode(authzCode);
        URI callbackURI;
        try {
            callbackURI = new URI(properties.getProperty(SSOAgentConstants.CALL_BACK_URL));
        } catch (URISyntaxException e) {
            throw new SSOAgentClientException("callback URL is not configured.");
        }
        AuthorizationGrant authorizationGrant = new AuthorizationCodeGrant(authorizationCode, callbackURI);
        ClientID clientID = new ClientID(properties.getProperty(SSOAgentConstants.CONSUMER_KEY));
        Secret clientSecret = new Secret(properties.getProperty(SSOAgentConstants.CONSUMER_SECRET));
        ClientAuthentication clientAuthentication = new ClientSecretBasic(clientID, clientSecret);
        URI tokenEndpoint;
        try {
            tokenEndpoint = new URI(properties.getProperty(SSOAgentConstants.OIDC_TOKEN_ENDPOINT));
        } catch (URISyntaxException e) {
            throw new SSOAgentClientException("token endpoint URL is not configured.");
        }

        TokenRequest tokenRequest = new TokenRequest(tokenEndpoint, clientAuthentication, authorizationGrant);
        TokenResponse tokenResponse = null;
        try {
            tokenResponse = TokenResponse.parse(tokenRequest.toHTTPRequest().send());
        } catch (com.nimbusds.oauth2.sdk.ParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        JSONObject requestObject = requestToJson(tokenRequest);
        JSONObject responseObject;

        if (!tokenResponse.indicatesSuccess()) {
            TokenErrorResponse errorResponse = tokenResponse.toErrorResponse();
            responseObject = errorResponse.toJSONObject();
            session.setAttribute("requestObject", requestObject);
            session.setAttribute("responseObject", responseObject);
            session.invalidate();
            response.sendRedirect("./");
        } else {
            AccessTokenResponse successResponse = tokenResponse.toSuccessResponse();
            responseObject = successResponse.toJSONObject();
            final String accessToken = successResponse.getTokens().getAccessToken().toString();

            session.setAttribute("requestObject", requestObject);
            session.setAttribute("responseObject", responseObject);
            if (accessToken != null) {
                session.setAttribute("accessToken", accessToken);
                String idToken = successResponse.getCustomParameters().get("id_token").toString();

                if (idToken != null) {
                    try {
                        JWTClaimsSet claimsSet = SignedJWT.parse(idToken).getJWTClaimsSet();
                        User user = new User(claimsSet.getSubject(), getUserAttributes(idToken));
                        session.setAttribute("idToken", idToken);
                        session.setAttribute("user", user);
                    } catch (ParseException e) {
                        throw new SSOAgentServerException("Error while parsing id_token.");
                    }
                }
                session.setAttribute("authenticated", true);

                TokenData tokenData = new TokenData();
                tokenData.setAccessToken(accessToken);
                tokenData.setIdToken(idToken);

                final String sessionId = UUID.randomUUID().toString();
                CommonUtils.TOKEN_STORE.put(sessionId, tokenData);
                final Cookie cookie = new Cookie("AppID", sessionId);
                cookie.setMaxAge(-1);
                cookie.setPath("/");
                response.addCookie(cookie);
            } else {
                session.invalidate();
            }
        }
    }

    private void handleAppIdCookieForLogout(HttpServletRequest request, HttpServletResponse response) {

        Optional<Cookie> appIdCookie = getAppIdCookie(request);

        if (appIdCookie.isPresent()) {
            CommonUtils.TOKEN_STORE.remove(appIdCookie.get().getValue());
            appIdCookie.get().setMaxAge(0);
            response.addCookie(appIdCookie.get());
        }
    }

    private boolean isLogout(HttpServletRequest request) {

        if (request.getParameterMap().isEmpty()) {
            return true;
        }
        if (request.getParameterMap().containsKey("sp") &&
                request.getParameterMap().containsKey("tenantDomain")) {
            return true;
        }
        return false;
    }

    private JSONObject requestToJson(TokenRequest tokenRequest) {

        JSONObject obj = new JSONObject();
        obj.appendField("tokenEndpoint", tokenRequest.toHTTPRequest().getURI().toString());
        obj.appendField("request body", tokenRequest.toHTTPRequest().getQueryParameters());
        return obj;
    }

    private Map<String, Object> getUserAttributes(String idToken) throws SSOAgentServerException {

        Map<String, Object> userClaimValueMap = new HashMap<>();
        try {
            JWTClaimsSet claimsSet = SignedJWT.parse(idToken).getJWTClaimsSet();
            Map<String, Object> customClaimValueMap = claimsSet.getClaims();

            for (String claim : customClaimValueMap.keySet()) {
                if (!SSOAgentConstants.OIDC_METADATA_CLAIMS.contains(claim)) {
                    userClaimValueMap.put(claim, customClaimValueMap.get(claim));
                }
            }
        } catch (ParseException e) {
            throw new SSOAgentServerException("Error while parsing JWT.");
        }
        return userClaimValueMap;
    }

    private boolean isLogin(HttpServletRequest request) {

        String authzCode = request.getParameter("code");
        return StringUtils.isNotBlank(authzCode);
    }

    private boolean isError(HttpServletRequest request) {

        String error = request.getParameter(SSOAgentConstants.ERROR);
        return StringUtils.isNotBlank(error);
    }

    private boolean checkOAuth(final HttpServletRequest request) {

        final HttpSession currentSession = request.getSession(false);

        return currentSession != null
                && currentSession.getAttribute("authenticated") != null
                && (boolean) currentSession.getAttribute("authenticated");
    }

    private void setTokenDataToSession(final HttpSession session, final TokenData storedTokenData) {

        session.setAttribute("authenticated", true);
        session.setAttribute("accessToken", storedTokenData.getAccessToken());
        session.setAttribute("idToken", storedTokenData.getIdToken());
    }

    private String getIndexPage(HttpSession session, Properties properties) {

        if (StringUtils.isNotBlank(properties.getProperty(SSOAgentConstants.INDEX_PAGE))) {
            return properties.getProperty(SSOAgentConstants.INDEX_PAGE);
        } else if (session != null) {
            return session.getServletContext().getContextPath();
        } else {
            return "./";
        }
    }
}
