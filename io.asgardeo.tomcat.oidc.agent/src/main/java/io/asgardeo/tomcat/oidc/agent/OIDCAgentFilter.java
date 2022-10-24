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

package io.asgardeo.tomcat.oidc.agent;

import com.nimbusds.oauth2.sdk.util.StringUtils;
import io.asgardeo.java.oidc.sdk.HTTPSessionBasedOIDCProcessor;
import io.asgardeo.java.oidc.sdk.SSOAgentConstants;
import io.asgardeo.java.oidc.sdk.bean.RequestContext;
import io.asgardeo.java.oidc.sdk.bean.SessionContext;
import io.asgardeo.java.oidc.sdk.config.model.OIDCAgentConfig;
import io.asgardeo.java.oidc.sdk.exception.SSOAgentClientException;
import io.asgardeo.java.oidc.sdk.exception.SSOAgentException;
import io.asgardeo.java.oidc.sdk.exception.SSOAgentServerException;
import io.asgardeo.java.oidc.sdk.request.OIDCRequestResolver;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * OIDCAgentFilter is the Filter class responsible for building
 * requests and handling responses for authentication, SLO and session
 * management for the OpenID Connect flows, using the io-asgardeo-oidc-sdk.
 * It is an implementation of the base class, {@link Filter}.
 * OIDCAgentFilter verifies if:
 * <ul>
 * <li>The request is a URL to skip
 * <li>The request is a Logout request
 * <li>The request is already authenticated
 * </ul>
 * <p>
 * and build and send the request, handle the response,
 * or forward the request accordingly.
 */
public class OIDCAgentFilter implements Filter {

    private static final Logger logger = LogManager.getLogger(OIDCAgentFilter.class);

    protected FilterConfig filterConfig = null;
    OIDCAgentConfig oidcAgentConfig;
    HTTPSessionBasedOIDCProcessor oidcManager;

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
            throw new SSOAgentException(e.getMessage(), e);
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
                handleException(request, response, e);
            }
            return;
        }

        if (requestResolver.isCallbackResponse()) {
            RequestContext requestContext = getRequestContext(request);
            if (requestContext == null) {
                handleException(request, response, new SSOAgentServerException("Request context is null."));
                return;
            }

            try {
                oidcManager.handleOIDCCallback(request, response);
            } catch (SSOAgentException e) {
                handleException(request, response, e);
                return;
            }
            // Check for logout scenario.
            if (requestResolver.isLogout()) {
                response.sendRedirect(oidcAgentConfig.getIndexPage());
                return;
            }
            String homePage = resolveTargetPage(request, requestContext);
            response.sendRedirect(homePage);
            return;
        }

        if (!isActiveSessionPresent(request)) {
            try {
                oidcManager.sendForLogin(request, response);
            } catch (SSOAgentException e) {
                handleException(request, response, e);
            }
        } else {
            filterChain.doFilter(request, response);
        }
    }

    private String resolveTargetPage(HttpServletRequest request, RequestContext requestContext) {

        String targetPage = "";
        if (StringUtils.isNotBlank(oidcAgentConfig.getHomePage())) {
            targetPage = oidcAgentConfig.getHomePage();
        } else if (requestContext != null && StringUtils.isNotBlank((CharSequence) requestContext.getParameter(
                SSOAgentConstants.REDIRECT_URI_KEY))) {
            targetPage = requestContext.getParameter(SSOAgentConstants.REDIRECT_URI_KEY).toString();
        } else if (StringUtils.isNotBlank(oidcAgentConfig.getIndexPage())) {
            targetPage = oidcAgentConfig.getIndexPage();
        }  else {
            String requestUrl = request.getRequestURL().toString();
            targetPage = requestUrl.substring(0, requestUrl.length() - request.getServletPath().length());
        }

        return targetPage;
    }

    private RequestContext getRequestContext(HttpServletRequest request) {

        HttpSession session = request.getSession(false);

        if (session != null && session.getAttribute(SSOAgentConstants.REQUEST_CONTEXT) != null) {
            return (RequestContext) request.getSession(false).getAttribute(SSOAgentConstants.REQUEST_CONTEXT);
        }
        return null;
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

    protected void handleException(HttpServletRequest request, HttpServletResponse response, SSOAgentException e)
            throws ServletException, IOException {

        String errorPage = oidcAgentConfig.getErrorPage();
        if (StringUtils.isBlank(errorPage)) {
            errorPage = buildErrorPageURL(oidcAgentConfig, request);
        }
        if (errorPage.trim().charAt(0) != '/') {
            errorPage = "/" + errorPage;
        }
        clearSession(request);
        logger.log(Level.FATAL, e.getMessage());
        request.setAttribute(SSOAgentConstants.AGENT_EXCEPTION, e);
        RequestDispatcher requestDispatcher = request.getServletContext().getRequestDispatcher(errorPage);
        requestDispatcher.forward(request, response);
    }

    private String buildErrorPageURL(OIDCAgentConfig oidcAgentConfig, HttpServletRequest request) {

        if (StringUtils.isNotBlank(oidcAgentConfig.getErrorPage())) {
            return oidcAgentConfig.getErrorPage();
        } else if (StringUtils.isNotBlank(oidcAgentConfig.getIndexPage())) {
            return oidcAgentConfig.getIndexPage();
        }
        return SSOAgentConstants.DEFAULT_CONTEXT_ROOT;
    }
}
