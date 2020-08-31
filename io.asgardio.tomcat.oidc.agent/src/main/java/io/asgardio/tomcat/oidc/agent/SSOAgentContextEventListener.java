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

import io.asgardio.java.oidc.sdk.SSOAgentConstants;
import io.asgardio.java.oidc.sdk.exception.SSOAgentClientException;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * An event listener class used to load the app.properties file configurations.
 */
public class SSOAgentContextEventListener implements ServletContextListener {

    private static final Logger logger = LogManager.getLogger(OIDCCallbackResponseHandler.class);

    private static Properties properties;

    /**
     * Get the properties of the sample
     *
     * @return Properties
     */
    public static Properties getProperties() {

        return properties;
    }

    public static String getPropertyByKey(final String key) {

        return properties.getProperty(key);
    }

    public void contextInitialized(ServletContextEvent servletContextEvent) {

        properties = new Properties();

        try {
            ServletContext servletContext = servletContextEvent.getServletContext();
            String propertyFileName = servletContext.getInitParameter(
                    SSOAgentConstants.APP_PROPERTY_FILE_PARAMETER_NAME);

            if (StringUtils.isNotBlank(propertyFileName)) {
                properties.load(servletContextEvent.getServletContext().
                        getResourceAsStream("/WEB-INF/classes/" + propertyFileName));
            } else {
                throw new SSOAgentClientException(SSOAgentConstants.APP_PROPERTY_FILE_PARAMETER_NAME
                        + " context-param is not specified in the web.xml");
            }

        } catch (IOException | SSOAgentClientException e) {
            logger.log(Level.FATAL, "Error while loading properties.", e);
        }
    }

    public void contextDestroyed(ServletContextEvent servletContextEvent) {

    }

}
