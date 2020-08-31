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

package io.asgardio.tomcat.oidc.agent.util;

import io.asgardio.java.oidc.sdk.SSOAgentConstants;
import org.apache.commons.lang.StringUtils;

import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public class Utils {

    public static String getIndexPage(HttpServletRequest request, Properties properties) {

        if (StringUtils.isNotBlank(properties.getProperty(SSOAgentConstants.INDEX_PAGE))) {
            return properties.getProperty(SSOAgentConstants.INDEX_PAGE);
        } else {
            return request.getContextPath();
        }
    }

    public static boolean isAuthenticated(final HttpServletRequest request) {

        final HttpSession currentSession = request.getSession(false);

        return currentSession != null
                && currentSession.getAttribute(SSOAgentConstants.AUTHENTICATED) != null
                && (boolean) currentSession.getAttribute(SSOAgentConstants.AUTHENTICATED);
    }
}
