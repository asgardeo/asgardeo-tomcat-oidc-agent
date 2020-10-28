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

import io.asgardio.java.oidc.sdk.HTTPSessionBasedOIDCProcessor;
import io.asgardio.java.oidc.sdk.SSOAgentConstants;
import io.asgardio.java.oidc.sdk.bean.SessionContext;
import io.asgardio.java.oidc.sdk.config.model.OIDCAgentConfig;
import io.asgardio.java.oidc.sdk.exception.SSOAgentClientException;
import io.asgardio.java.oidc.sdk.exception.SSOAgentException;
import io.asgardio.java.oidc.sdk.exception.SSOAgentServerException;
import io.asgardio.java.oidc.sdk.request.OIDCRequestResolver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * HTTPSessionBasedOIDCAgentFilter is the Filter class responsible for building
 * requests and handling responses for authentication, SLO and session
 * management for the OpenID Connect flows, using the io-asgardio-oidc-sdk.
 * It is an implementation of the base class, {@link Filter}.
 * HTTPSessionBasedOIDCAgentFilter verifies if:
 * <ul>
 * <li>The request is a URL to skip
 * <li>The request is a Logout request
 * <li>The request is already authenticated
 * </ul>
 * <p>
 * and build and send the request, handle the response,
 * or forward the request accordingly.
 *
 * @version 0.1.1
 * @since 0.1.1
 */
public class HTTPSessionBasedOIDCAgentFilter implements Filter {

    private static final Logger logger = LogManager.getLogger(OIDCAgentFilter.class);

    protected FilterConfig filterConfig = null;
    OIDCAgentConfig oidcAgentConfig;
    HTTPSessionBasedOIDCProcessor oidcManager;
    SessionContext sessionContext = new SessionContext();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

        this.filterConfig = filterConfig;
        ServletContext servletContext = filterConfig.getServletContext();
        if (servletContext.getAttribute(SSOAgentConstants.CONFIG_BEAN_NAME) instanceof OIDCAgentConfig) {
            this.oidcAgentConfig = (OIDCAgentConfig) servletContext.getAttribute(SSOAgentConstants.CONFIG_BEAN_NAME);
        }
        try {
            this.oidcManager = new HTTPSessionBasedOIDCProcessor(oidcAgentConfig);
        } catch (SSOAgentClientException e) {
            e.printStackTrace(); //TODO
        }
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        OIDCRequestResolver requestResolver = new OIDCRequestResolver(request, oidcAgentConfig);

        if (requestResolver.isSkipURI()) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        if (requestResolver.isLogoutURL()) {
            try {
                oidcManager.logout(request, response);
            } catch (SSOAgentException e) {
                handleException(request, e);
            }
            return;
        }

        if (requestResolver.isCallbackResponse()) {
            try {
                oidcManager.handleOIDCCallback(request, response);
            } catch (SSOAgentServerException e) {
                handleException(request, e);
            }
            response.sendRedirect("home.jsp");
            return;
        }

        if (!isActiveSessionPresent(request)) {
            try {
                oidcManager.sendForLogin(request, response);
            } catch (SSOAgentException e) {
                handleException(request, e);
            }
        } else {
            filterChain.doFilter(request, response);
        }
    }

    @Override
    public void destroy() {

    }

    boolean isActiveSessionPresent(HttpServletRequest request) {

        HttpSession currentSession = request.getSession(false);

        return currentSession != null
                && currentSession.getAttribute(SSOAgentConstants.SESSION_CONTEXT) != null
                && currentSession.getAttribute(SSOAgentConstants.SESSION_CONTEXT) instanceof SessionContext;
    }

    void clearSession(HttpServletRequest request) {

        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    protected void handleException(HttpServletRequest request, SSOAgentException e) throws SSOAgentException {

        clearSession(request);
        throw e;
    }
}
