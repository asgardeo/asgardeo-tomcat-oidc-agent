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
import com.nimbusds.oauth2.sdk.token.AccessToken;
import io.asgardio.java.oidc.sdk.bean.User;
import io.asgardio.java.oidc.sdk.exception.SSOAgentClientException;
import io.asgardio.java.oidc.sdk.exception.SSOAgentServerException;
import io.asgardio.java.oidc.sdk.util.SSOAgentConstants;
import net.minidev.json.JSONObject;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

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

        if (isError(request)) {
            clearSession(request);
            response.sendRedirect(getIndexPage(request, properties));
        } else if (isLogout(request)) {
            clearSession(request);
            response.sendRedirect(getIndexPage(request, properties));
        } else if (isAuthorizationCodeResponse(request)) {
            try {
                boolean isAuthenticated = handleAuthentication(request, response);
                if (isAuthenticated) {
                    response.sendRedirect("home.jsp"); //TODO: target page
                } else {
                    request.getSession().invalidate();
                    // redirect to index TODO error.jsp
                    response.sendRedirect(getIndexPage(request, properties));
                }
            } catch (SSOAgentServerException | SSOAgentClientException e) {
                response.sendRedirect(getIndexPage(request, properties));
            }
        } else {
            //log: invalid scenario
            clearSession(request);
            response.sendRedirect(getIndexPage(request, properties));
        }
    }

    private void clearSession(HttpServletRequest request) {

        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    private boolean handleAuthentication(final HttpServletRequest request, final HttpServletResponse response)
            throws SSOAgentServerException, SSOAgentClientException, IOException {

        HttpSession session = request.getSession();
        session.invalidate();
        session = request.getSession();

        final Properties properties = SSOAgentContextEventListener.getProperties();

        final String authzCode = request.getParameter("code");
//      Use  AuthorizationResponse authorizationResponse
        request.getSession()
                .setAttribute(SSOAgentConstants.SESSION_STATE, request.getParameter(SSOAgentConstants.SESSION_STATE));

        TokenRequest tokenRequest = getTokenRequest(properties, authzCode);
        TokenResponse tokenResponse = getTokenResponse(tokenRequest);

        if (!tokenResponse.indicatesSuccess()) {
            handleErrorTokenResponse(tokenRequest, tokenResponse);
            return false;
        } else {
            handleSuccessTokenResponse(session, tokenResponse);
            return true;
        }
    }

    private void handleSuccessTokenResponse(HttpSession session, TokenResponse tokenResponse)
            throws SSOAgentServerException {

        AccessTokenResponse successResponse = tokenResponse.toSuccessResponse();
        AccessToken accessToken = successResponse.getTokens().getAccessToken();

        session.setAttribute("accessToken", accessToken); //use constants
        String idToken = successResponse.getCustomParameters().get("id_token").toString(); // parse to JWT

        if (idToken == null) {
            throw new SSOAgentServerException("null id token"); //TODO log
        }
        //TODO validate IdToken (Signature, ref. spec)
        try {
            JWTClaimsSet claimsSet = SignedJWT.parse(idToken).getJWTClaimsSet();
            User user = new User(claimsSet.getSubject(), getUserAttributes(idToken));
            session.setAttribute("idToken", idToken);
            session.setAttribute("user", user);
            session.setAttribute("authenticated", true);
        } catch (ParseException e) {
            throw new SSOAgentServerException("Error while parsing id_token.");
        }
    }

    private void handleErrorTokenResponse(TokenRequest tokenRequest, TokenResponse tokenResponse) {

        TokenErrorResponse errorResponse = tokenResponse.toErrorResponse();
        JSONObject requestObject = requestToJson(tokenRequest);
        JSONObject responseObject = errorResponse.toJSONObject();
        //log requestObject, responseObject
    }

    private TokenResponse getTokenResponse(TokenRequest tokenRequest) {

        TokenResponse tokenResponse = null;
        try {
            tokenResponse = TokenResponse.parse(tokenRequest.toHTTPRequest().send());
        } catch (com.nimbusds.oauth2.sdk.ParseException e) {
            e.printStackTrace(); //TODO
        } catch (IOException e) {
            e.printStackTrace();
        }
        return tokenResponse;
    }

    private TokenRequest getTokenRequest(Properties properties, String authzCode) throws SSOAgentClientException {

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

        return new TokenRequest(tokenEndpoint, clientAuthentication, authorizationGrant);
    }

    private boolean isLogout(HttpServletRequest request) {

        if (request.getParameterMap().isEmpty()) { //TODO: revise
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

    private boolean isAuthorizationCodeResponse(HttpServletRequest request) {

        String authzCode = request.getParameter("code");
        return StringUtils.isNotBlank(authzCode);
    }

    private boolean isError(HttpServletRequest request) {

        String error = request.getParameter(SSOAgentConstants.ERROR);
        return StringUtils.isNotBlank(error);
    }

    private String getIndexPage(HttpServletRequest request, Properties properties) { //TODO: mv to Util

        if (StringUtils.isNotBlank(properties.getProperty(SSOAgentConstants.INDEX_PAGE))) {
            return properties.getProperty(SSOAgentConstants.INDEX_PAGE);
        } else {
            return request.getContextPath(); //TODO: verify whether context url string is returned
        }
    }
}
