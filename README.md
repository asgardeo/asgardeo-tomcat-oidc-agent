# Asgardio OIDC SDK for Java

The Asgardio OIDC SDK for Java enables software developers to integrate OIDC based SSO authentication with Java Web
 applications. The SDK is built on top of the Apache Oltu Oauth2 library which allows Java developers to develop cross
 -domain
  single sign-on and federated access control solutions with minimum hassle.

## Trying out the sample

### Prerequisites
1. WSO2 Identity Server and it's [prerequisites](https://is.docs.wso2.com/en/next/setup/installing-the-product/).

A sample app for demonstrating OIDC based authentication/authorization, logout and attribute retrieval is hosted at:
[TODO link]

You can download the pre-built oidc-sample-app.war from [TODO link]

### Running the SampleApp

In order to secure the webapp using OIDC, please follow these steps 
 
1. Start the WSO2 IS. 
2. Access WSO2 IS management console and create a service provider (ex:- oidc-sample-app)
   
   For the service provider, configure Oauth/OpenID Connect under Inbound Authentication Configuration. In this
    configuration,
   use following parameters and options,
     
       Callback URL - http://localhost:8080/oidc-sample-app/oauth2client


   Keep the other default settings as it is and save the configuration.
   
   Next, expand the [Claim Configuration](https://is.docs.wso2.com/en/latest/learn/configuring-claims-for-a-service-provider/#configuring-claims-for-a-service-provider) section. In this configuration, Set the following config and add the claims you 
   need to retrieve (ex: http://wso2.org/claims/lastname) from the web app.
   
       Select Claim mapping Dialect - Use Local Claim Dialect
       
   See the example claim config below.
   ![Claim Config](https://user-images.githubusercontent.com/15249242/90488235-38d45580-e159-11ea-8beb-52d6b5c35034.png)

       
3. Deploy the application, `oidc-sample-app.war` using Apache Tomcat.
4. Try out the application by accessing the `http://localhost:8080/oidc-sample-app/index.html`.

   By default, the application runs on url `http://localhost:8080/oidc-sample-app/`
 

![Recordit GIF](http://g.recordit.co/BKqufkpZW1.gif)

**NOTE:** Some browsers do not support cookie creation for naked host names (ex:- localhost). SSO functionality
 require cookies in the browser. 

In that case, use `localhost.com` host name for the sample application. You will require to edit the SampleApp
.properties file in <TOMCAT_HOME>/webapps/oidc-sample-app/WEB-INF/classes directory and set the following:

`callBackUrl=http://localhost.com:8080/oidc-sample-app/oauth2client`

and update the callback URL in the Identity Server Service Provider configurations accordingly.

You will also require to add this entry 
to `hosts` file. For windows this file locations is at `<Windows-Installation-Drive>\Windows\System32\drivers\etc
\hosts`.
For Linux/Mac OS, this file location is at `/etc/hosts`.

## How it works

In the oidc-sample-app, we have two pages. A landing page (index.html) which we have not secured, and a secondary
 page (home.jsp) which we have secured.

In the oidc-sample-app.properties file in the `identity-agent-sso/resources/oidc-sample-app/src/main/resources` directory, we
 have set the /oidc-sample-app/index.html as the index page via the following property:

    indexPage=/oidc-sample-app/index.html

Hence, the sso agent regards the index.html page as the landing page and would be added to the skipURIs. Then, the
 index page would be regarded as a page that is not secured.

When a logout sequence is initiated, the sso agent would redirect the user to this exact page which is configured via
 the `indexPage` property.

In the **index.html** page of the oidc-sample-app, the login button would send a request to the **home.jsp** page.
  This request would first engage the **OIDCAuthorizationFilter** which is specified in the **web.xml** file in the
 `identity-agent-sso/resources/sample-app-oidc/src/main/webapp/WEB-INF` directory. There, it would check if there is
  an authenticated session in place. If the session is authenticated, the request would be handled by the
   **DispatchClientServlet** and would forward the user to the **home.jsp** page.
   
   In case the current session is not authenticated, the filter would initiate an authentication request and redirect
    the user for authentication. Upon successful authentication, the request would engage the 
    **DispatchClientServlet** and the user would be redirected to the **home.jsp** page.


In the **home.jsp** file, we have added the following to trigger a logout flow:

``<a href='<%=properties.getProperty(SSOAgentConstants.OIDC_LOGOUT_ENDPOINT)%>?post_logout_redirect_uri=<%=properties.getProperty("post_logout_redirect_uri")%>&id_token_hint=<%=idToken%>&session_state=<%=sessionState%>'>Logout</a>``

After successful logout, the user would be
 redirected to the page configured via the `indexPage` property previously discussed.


## Integrating OIDC into your Java application

### Getting Started

These instructions will guide you on integrating OIDC into your Java application with the Asgardio OIDC SDK.
This allows the developers to turn a Java application into a SP (Service Provider) that can be connected to an IdP
 (Identity Provider) which can then be secured with OIDC.
 

The structure of the web app boilerplate we would be using is as follows:

[![INSERT YOUR GRAPHIC HERE](https://miro.medium.com/max/1400/1*M9-eI8gcUugJD_6u7PXN1Q.png)]()

### Configuring the web app

Starting with the pom.xml, the following dependencies should be added for the webApp for it to be using the OIDC SDK.

Install it as a maven dependency:
```
<dependency>
    <groupId>io.asgardio.tomcat.oidc.agent</groupId>
    <artifactId>io.asgardio.tomcat.oidc.agent</artifactId>
    <version>0.1.1-SNAPSHOT</version> [TODO]
</dependency>
```
The SDK is hosted at the WSO2 Internal Repository. Point to the repository as follows:


```
<repositories>
    <repository>
        <id>wso2.releases</id>
        <name>WSO2 internal Repository</name>
        <url>http://maven.wso2.org/nexus/content/repositories/releases/</url>
        <releases>
            <enabled>true</enabled>
            <updatePolicy>daily</updatePolicy>
            <checksumPolicy>ignore</checksumPolicy>
        </releases>
    </repository>
</repositories>
```
Next, the webapp itself has two pages, index.html and home.jsp, and a web.xml file.

The index.html contains a login button which we would use to forward the user to the secured page.

`<form method="post" action="home.jsp">`

The home.jsp page is a page which we want to secure i.e. in case there are no active sessions, the http://localhost
:8080/oidc-sample-app/home.jsp should not be accessible. In the sampleApp we are using, if there is no active session in
 place, we would redirect the user for authentication. In the home.jsp, there is a logout link which will be used to
  create a logout request.

`<a href='logout'>Logout</a>`

Before the web.xml configurations, we will look at adding the resources files.

In the oidc-sample-app, create a file named oidc-sample-app.properties in the resources directory. The oidc-sample-app.properties
 file contains properties similar to the following:

```
consumerKey=KE4OYeY_gfYwzQbJa9tGhj1hZJMa
consumerSecret=_ebDU3prFV99JYgtbnknB0z0dXoa
skipURIs=
indexPage=
callBackUrl=http://localhost:8080/oidc-sample-app/oauth2client
scope=openid internal_application_mgt_view
grantType=code
authorizeEndpoint=https://localhost:9443/oauth2/authorize
logoutEndpoint=https://localhost:9443/oidc/logout
sessionIFrameEndpoint=https://localhost:9443/oidc/checksession
tokenEndpoint=https://localhost:9443/oauth2/token
postLogoutRedirectURI=http://localhost:8080/oidc-sample-app/oauth2client
api_endpoint=
```
These properties are required for the OIDC SDK to communicate with the WSO2 Identity Server.

Next, we need to find and set JKS properties required for IS server communication.  For that, create a file named jks
.properties in the resources directory. The content of the jks.properties file should be similar to:

```
keystorename=wso2carbon.jks
keystorepassword=wso2carbon
```

Finally, copy and paste the following web.xml configurations to the WEB-INF/web.xml file. Make sure that you update
 param-values of the context-params,

`<param-name>app-property-file</param-name>`

`<param-name>jks-property-file</param-name>`

to match yours.

```
<?xml version="1.0" encoding="UTF-8"?>

<!--
  ~ Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
  ~
  ~ WSO2 Inc. licenses this file to you under the Apache License,
  ~ Version 2.0 (the "License"); you may not use this file except
  ~ in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~ http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing,
  ~ software distributed under the License is distributed on an
  ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  ~ KIND, either express or implied.  See the License for the
  ~ specific language governing permissions and limitations
  ~ under the License.
  -->

<web-app xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" id="SampleApp"
         version="2.5"
         xmlns="http://java.sun.com/xml/ns/javaee"
         xsi:schemaLocation="http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd">

    <display-name>oidc-sample-app</display-name>

    <servlet>
        <servlet-name>OAuth2ClientServlet</servlet-name>
        <servlet-class>io.asgardio.tomcat.oidc.agent.OIDCCallbackResponseHandler</servlet-class>
        <load-on-startup>0</load-on-startup>
    </servlet>
    <servlet-mapping>
        <servlet-name>OAuth2ClientServlet</servlet-name>
        <url-pattern>/oauth2client</url-pattern>
    </servlet-mapping>

    <servlet>
        <servlet-name>OAuth2LogoutServlet</servlet-name>
        <servlet-class>io.asgardio.tomcat.oidc.agent.LogoutServlet</servlet-class>
        <load-on-startup>0</load-on-startup>
    </servlet>
    <servlet-mapping>
        <servlet-name>OAuth2LogoutServlet</servlet-name>
        <url-pattern>/logout</url-pattern>
    </servlet-mapping>

    <filter>
        <filter-name>AuthorizationFilter</filter-name>
        <filter-class>io.asgardio.tomcat.oidc.agent.OIDCAuthorizationFilter</filter-class>
    </filter>
    <filter-mapping>
        <filter-name>AuthorizationFilter</filter-name>
        <url-pattern>*.jsp</url-pattern>
    </filter-mapping>
    <filter-mapping>
        <filter-name>AuthorizationFilter</filter-name>
        <url-pattern>*.html</url-pattern>
    </filter-mapping>

    <listener>
        <listener-class>io.asgardio.tomcat.oidc.agent.SSOAgentContextEventListener</listener-class>
    </listener>
    <context-param>
        <param-name>app-property-file</param-name>
        <param-value>oidc-sample-app.properties</param-value>
    </context-param>

    <listener>
        <listener-class>io.asgardio.tomcat.oidc.agent.JKSLoader</listener-class>
    </listener>
    <context-param>
        <param-name>jks-property-file</param-name>
        <param-value>jks.properties</param-value>
    </context-param>
</web-app>

```
### Retrieving User Attributes

The web app needs to be configured to read the attributes sent from the Identity Server upon successful
 authentication. In the oidc-sample-app, we would customize the home.jsp file as follows to retrieve the user
  attributes.
 
 ```
.
.
</head>
%
   final HttpSession currentSession = request.getSession(false);
    final String idToken = (String) currentSession.getAttribute("idToken");
    
    String name = null;
    Map<String, Object> customClaimValueMap = new HashMap<>();
    
    if (idToken != null) {
        try {
            final User user = (User) currentSession.getAttribute("user");
            customClaimValueMap = user.getAttributes();
            name = user.getSubject();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
%>
<body>
.
.
```
Then, we would use the `customClaimValueMap` in the **home.jsp** to display the user attributes via a table:

```
<% if (!customClaimValueMap.isEmpty()) { %>
        <div class="element-padding">
            <div class="element-padding">
                <h3 align="center">User Details</h3>
            </div>
            <table class="center">
                <tbody>
                <% for (String claim: customClaimValueMap.keySet()) { %>
                <tr>
                    <td><%=claim%>
                    </td>
                    <td><%=customClaimValueMap.get(claim).toString()%>
                    </td>
                </tr>
                <% } %>
                </tbody>
            </table>
            <% } else { %>
            <p align="center">No user details Available. Configure SP Claim Configurations.</p>
            <% } %>
        </div>
```
After the above configurations, your app would be able to try out the authentication, logout and attribute 
retrieval flows with OIDC.
 
## Installing the SDK [TODO]

### Github
The SDK is hosted on github. You can download it from:
- Latest release: [TODO]
- Master repo: [TODO]

### Building from the source

If you want to build **asgardio-tomcat-oidc-agent** from the source code:

1. Install Java 8
2. Install Apache Maven 3.x.x (https://maven.apache.org/download.cgi#)
3. Get a clone or download the source from this repository (https://github.com/asgardio/asgardio-tomcat-oidc-agent.git)
4. Run the Maven command ``mvn clean install`` from the ``asgardio-tomcat-oidc-agent`` directory.

### Maven

Install it as a maven dependency:
```
<dependency>
    <groupId>io.asgardio.tomcat.oidc.agent</groupId>
    <artifactId>io.asgardio.tomcat.oidc.agent</artifactId>
    <version>0.1.1-SNAPSHOT</version> [TODO]
</dependency>
```
The SDK is hosted at the WSO2 Internal Repository. Point to the repository as follows:


```
<repositories>
    <repository>
        <id>wso2.releases</id>
        <name>WSO2 internal Repository</name>
        <url>http://maven.wso2.org/nexus/content/repositories/releases/</url>
        <releases>
            <enabled>true</enabled>
            <updatePolicy>daily</updatePolicy>
            <checksumPolicy>ignore</checksumPolicy>
        </releases>
    </repository>
</repositories>
```

## Contributing

Please read [Contributing to the Code Base](http://wso2.github.io/) for details on our code of conduct, and the
 process for submitting pull requests to us.
 
### Reporting Issues
We encourage you to report issues, improvements, and feature requests creating [git Issues](https://github.com/asgardio/asgardio-tomcat-oidc-agent/issues).

Important: And please be advised that security issues must be reported to security@wso2.com, not as GitHub issues, 
in order to reach the proper audience. We strongly advise following the WSO2 Security Vulnerability Reporting Guidelines
 when reporting the security issues.

## Versioning

For the versions available, see the [tags on this repository](https://github.com/asgardio/asgardio-tomcat-oidc-agent/tags). 

## Authors


See also the list of [contributors](https://github.com/asgardio/asgardio-tomcat-oidc-agent/graphs/contributors) who
 participated in this project.

## License

This project is licensed under the Apache License 2.0 under which WSO2 Carbon is distributed. See the [LICENSE
](LICENSE) file for details.

