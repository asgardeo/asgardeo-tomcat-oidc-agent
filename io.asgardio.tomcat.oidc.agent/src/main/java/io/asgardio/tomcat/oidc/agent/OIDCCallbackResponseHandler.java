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
import com.nimbusds.oauth2.sdk.AbstractRequest;
import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.AuthorizationErrorResponse;
import com.nimbusds.oauth2.sdk.AuthorizationGrant;
import com.nimbusds.oauth2.sdk.AuthorizationResponse;
import com.nimbusds.oauth2.sdk.AuthorizationSuccessResponse;
import com.nimbusds.oauth2.sdk.TokenErrorResponse;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.http.ServletUtils;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import io.asgardio.java.oidc.sdk.SSOAgentConstants;
import io.asgardio.java.oidc.sdk.bean.User;
import io.asgardio.java.oidc.sdk.exception.SSOAgentClientException;
import io.asgardio.java.oidc.sdk.exception.SSOAgentServerException;
import net.minidev.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

import static io.asgardio.tomcat.oidc.agent.util.Utils.getIndexPage;

/**
 * OIDCCallbackResponseHandler is the Servlet class for handling
 * OIDC callback responses. It is extended from the base class, {@link HttpServlet}.
 *
 * @version     0.1.1
 * @since       0.1.1
 */
public class OIDCCallbackResponseHandler extends HttpServlet {

    private static final Logger logger = LogManager.getLogger(OIDCCallbackResponseHandler.class);

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

        if (!isError(request) && isAuthorizationCodeResponse(request)) {
            logger.log(Level.INFO, "Handling the OIDC Authorization response.");
            try {
                boolean isAuthenticated = handleAuthentication(request, response);
                if (isAuthenticated) {
                    logger.log(Level.INFO, "Authentication successful. Redirecting to the target page.");
                    response.sendRedirect("home.jsp"); //TODO: target page
                } else {
                    logger.log(Level.ERROR, "Authentication failed. Invalidating the session.");
                    request.getSession().invalidate();
                    // redirect to index TODO error.jsp
                    response.sendRedirect(getIndexPage(request, properties));
                }
            } catch (SSOAgentServerException | SSOAgentClientException e) {
                response.sendRedirect(getIndexPage(request, properties));
            }
        } else {
            logger.log(Level.INFO, "Clearing the active session and redirecting.");
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

        AuthorizationResponse authorizationResponse;
        AuthorizationCode authorizationCode;
        AuthorizationSuccessResponse successResponse;
        TokenRequest tokenRequest;
        TokenResponse tokenResponse;

        try {
            authorizationResponse = AuthorizationResponse.parse(ServletUtils.createHTTPRequest(request));

            if (!authorizationResponse.indicatesSuccess()) {
                handleErrorAuthorizationResponse(authorizationResponse);
                return false;
            } else {
                successResponse = authorizationResponse.toSuccessResponse();
                authorizationCode = successResponse.getAuthorizationCode();
            }
            tokenRequest = getTokenRequest(properties, authorizationCode);
            tokenResponse = getTokenResponse(tokenRequest);

            if (!tokenResponse.indicatesSuccess()) {
                handleErrorTokenResponse(tokenRequest, tokenResponse);
                return false;
            } else {
                handleSuccessTokenResponse(session, tokenResponse);
                return true;
            }
        } catch (com.nimbusds.oauth2.sdk.ParseException e) {
            logger.error(e.getMessage(), e);
            return false;
        }
    }

    private void handleSuccessTokenResponse(HttpSession session, TokenResponse tokenResponse)
            throws SSOAgentServerException {

        AccessTokenResponse successResponse = tokenResponse.toSuccessResponse();
        AccessToken accessToken = successResponse.getTokens().getAccessToken();

        session.setAttribute(SSOAgentConstants.ACCESS_TOKEN, accessToken);
        String idToken =
                successResponse.getCustomParameters().get(SSOAgentConstants.ID_TOKEN).toString();

        if (idToken == null) {
            logger.log(Level.ERROR, "id_token is null.");
            throw new SSOAgentServerException("null id token");
        }
        //TODO validate IdToken (Signature, ref. spec)
        try {
            JWTClaimsSet claimsSet = SignedJWT.parse(idToken).getJWTClaimsSet();
            User user = new User(claimsSet.getSubject(), getUserAttributes(idToken));
            session.setAttribute(SSOAgentConstants.ID_TOKEN, idToken);
            session.setAttribute(SSOAgentConstants.USER, user);
            session.setAttribute(SSOAgentConstants.AUTHENTICATED, true);
        } catch (ParseException e) {
            throw new SSOAgentServerException("Error while parsing id_token.");
        }
    }

    private void handleErrorTokenResponse(TokenRequest tokenRequest, TokenResponse tokenResponse) {

        TokenErrorResponse errorResponse = tokenResponse.toErrorResponse();
        JSONObject requestObject = requestToJson(tokenRequest);
        JSONObject responseObject = errorResponse.toJSONObject();
        logger.log(Level.INFO, "Request object for the error response: ", requestObject);
        logger.log(Level.INFO, "Error response object: ", responseObject);
    }

    private void handleErrorAuthorizationResponse(AuthorizationResponse authzResponse) {

        AuthorizationErrorResponse errorResponse = authzResponse.toErrorResponse();
        JSONObject responseObject = errorResponse.getErrorObject().toJSONObject();
        logger.log(Level.INFO, "Error response object: ", responseObject);
    }

    private TokenResponse getTokenResponse(TokenRequest tokenRequest) {

        TokenResponse tokenResponse = null;
        try {
            tokenResponse = TokenResponse.parse(tokenRequest.toHTTPRequest().send());
        } catch (com.nimbusds.oauth2.sdk.ParseException | IOException e) {
            logger.log(Level.ERROR, "Error while parsing token response.", e);
        }
        return tokenResponse;
    }

    private TokenRequest getTokenRequest(Properties properties, AuthorizationCode authorizationCode)
            throws SSOAgentClientException {

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

    private JSONObject requestToJson(AbstractRequest request) {

        JSONObject obj = new JSONObject();
        obj.appendField("tokenEndpoint", request.toHTTPRequest().getURI().toString());
        obj.appendField("request body", request.toHTTPRequest().getQueryParameters());
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

        AuthorizationResponse authorizationResponse;
        try {
            authorizationResponse = AuthorizationResponse.parse(ServletUtils.createHTTPRequest(request));
        } catch (com.nimbusds.oauth2.sdk.ParseException | IOException e) {
            logger.log(Level.ERROR, "Error occurred while parsing the authorization response.", e);
            return false;
        }
        if (!authorizationResponse.indicatesSuccess()) {
            handleErrorAuthorizationResponse(authorizationResponse);
            return false;
        }
        if (!authorizationResponse.toParameters().containsKey("code")) {
            return false;
        }
        return true;
    }

    private boolean isError(HttpServletRequest request) {

        String error = request.getParameter(SSOAgentConstants.ERROR);
        return StringUtils.isNotBlank(error);
    }
}
