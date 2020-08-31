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

import com.nimbusds.oauth2.sdk.AuthorizationRequest;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.id.ClientID;
import io.asgardio.java.oidc.sdk.SSOAgentConstants;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static io.asgardio.tomcat.oidc.agent.util.Utils.getIndexPage;
import static io.asgardio.tomcat.oidc.agent.util.Utils.isAuthenticated;

/**
 * A Filter class used to check sessions and secure pages.
 */
public class OIDCAuthorizationFilter implements Filter {

    private static final Logger logger = LogManager.getLogger(OIDCAuthorizationFilter.class);

    protected FilterConfig filterConfig = null;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

        this.filterConfig = filterConfig;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse; //TODO: use servlet request class

        Properties properties = SSOAgentContextEventListener.getProperties();

        if (isURLtoSkip(request, properties) || isAuthenticated(request)) {
            filterChain.doFilter(request, response);
        } else {
            try {
                AuthorizationRequest authorizationRequest = buildAuthorizationRequest(properties);
                response.sendRedirect(authorizationRequest.toURI().toString());
            } catch (URISyntaxException exception) {
                logger.error(exception.getMessage(), exception);
                String indexPage = getIndexPage(request, properties);
                response.sendRedirect(indexPage);
            }
        }
    }

    @Override
    public void destroy() {

    }

    private AuthorizationRequest buildAuthorizationRequest(Properties properties)
            throws URISyntaxException {

        String consumerKey = properties.getProperty(SSOAgentConstants.CONSUMER_KEY);
        String authzEndpoint = properties.getProperty(SSOAgentConstants.OAUTH2_AUTHZ_ENDPOINT);
        String scope = properties.getProperty(SSOAgentConstants.SCOPE);
        String callBackUrl = properties.getProperty(SSOAgentConstants.CALL_BACK_URL);

        ResponseType responseType = new ResponseType(ResponseType.Value.CODE);
        ClientID clientID = new ClientID(consumerKey);
        Scope authScope = new Scope(scope);
        URI callBackURI = new URI(callBackUrl);
        URI authorizationEndpoint = new URI(authzEndpoint);

        AuthorizationRequest authzRequest = new AuthorizationRequest.Builder(responseType, clientID)
                .scope(authScope)
                .redirectionURI(callBackURI)
                .endpointURI(authorizationEndpoint)
                .build();
        return authzRequest;

    }

    private boolean isURLtoSkip(HttpServletRequest request, Properties properties) {

        Set<String> skipURIs = getSkipURIs(properties);
        return skipURIs.contains(request.getRequestURI());
    }

    private Set<String> getSkipURIs(Properties properties) {

        Set<String> skipURIs = new HashSet<String>();
        String skipURIsString = properties.getProperty(SSOAgentConstants.SKIP_URIS);
        if (StringUtils.isNotBlank(skipURIsString)) {
            String[] skipURIArray = skipURIsString.split(",");
            for (String skipURI : skipURIArray) {
                skipURIs.add(skipURI);
            }
        }
        if (StringUtils.isNotBlank(properties.getProperty(SSOAgentConstants.INDEX_PAGE))) {
            skipURIs.add(properties.getProperty(SSOAgentConstants.INDEX_PAGE));
        }
        return skipURIs;
    }
}
