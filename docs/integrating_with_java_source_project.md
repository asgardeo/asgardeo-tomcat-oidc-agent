## Integrating OpenID Connect into your Java Source Project

In this tutorial, we will see how you can integrate Asgardeo Tomcat OIDC Agent into your Java source project.
You can refer to the [oidc-sample-app source code](../io.asgardeo.tomcat.oidc.sample) for a complete implementation.

  * [Prerequisites](#prerequisites)
  * [Configuring the sample](#configuring-the-sample)
  * [Enable login](#enable-login)
  * [Enable logout](#enable-logout)
  * [Retrieving user attributes](#retrieving-user-attributes)

### Prerequisites
1. [Maven](https://maven.apache.org/download.cgi) 3.6.x or higher

These instructions will guide you on integrating OpenID Connect into your Java application with the Asgardeo Tomcat OIDC
 Agent for Java.
This allows an application (i.e. Service Provider) to connect with an IDP using the Asgardeo Tomcat OIDC Agent for Java.

A sample application is included in 
https://github.com/asgardeo/asgardeo-tomcat-oidc-agent/tree/master/io.asgardeo.tomcat.oidc.sample
 which we would use for the following section. 
Here, we are using the sample as a reference only, we can follow the same approach to build our own app as well.
The structure of the sample would be as follows:

[![INSERT YOUR GRAPHIC HERE](https://miro.medium.com/max/1400/1*M9-eI8gcUugJD_6u7PXN1Q.png)]()

### Configuring the sample

1. Starting with the pom.xml, the following dependencies should be added for the webApp to be using the OIDC SDK.
      ```xml
      <dependency>
          <groupId>io.asgardeo.tomcat.oidc.agent</groupId>
          <artifactId>io.asgardeo.tomcat.oidc.agent</artifactId>
          <version>0.1.29</version>
      </dependency>
      ```

2. Before the web.xml configurations, we will look at adding the resources files.
   In the oidc-sample-app, create a file named oidc-sample-app.properties in the `src/main/resources` directory. The 
   oidc-sample-app.properties file contains properties similar to the following:

      ```text
      consumerKey=<OAuth Client Key>
      consumerSecret=<OAuth Client Secret>
      skipURIs=/oidc-sample-app/index.html
      indexPage=
      errorPage=
      logoutURL=logout
      callBackURL=http://localhost:8080/oidc-sample-app/oauth2client
      scope=openid,address,email,profile
      #grantType=code
      authorizeEndpoint=https://api.asgardeo.io/t/org_name/oauth2/authorize
      logoutEndpoint=https://api.asgardeo.io/t/org_name/oidc/logout
      #sessionIFrameEndpoint=https://api.asgardeo.io/t/org_name/oidc/checksession
      tokenEndpoint=https://api.asgardeo.io/t/org_name/oauth2/token
      issuer=https://api.asgardeo.io/t/org_name/oauth2/token
      jwksEndpoint=https://api.asgardeo.io/t/org_name/oauth2/jwks
      postLogoutRedirectURI=http://localhost:8080/oidc-sample-app/index.html
      trustedAudience=http://localhost:8080/sample-app
      signatureAlgorithm=RS256
      ```
A comprehensive list of the properties can be found in the [Configuration Catalog](../io.asgardeo.tomcat.oidc.sample/src/main/resources/configuration-catalog.md).
    These properties are required for the OIDC SDK to communicate with Asgardeo.

3. Next, we need to find and set JKS properties required for IS server communication.  For that, create a file named jks
      .properties in the `src/main/resources` directory. The content of the jks.properties file should be similar to:
      
      ```text
      keystorename=wso2carbon.jks
      keystorepassword=wso2carbon
      ```
   


4. Finally, copy and paste the following configurations to the `<src/main/webapp/WEB-INF/web.xml` file. 

   ```xml
   
   <?xml version="1.0" encoding="UTF-8"?>
     <web-app xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" id="SampleApp"
              version="2.5"
              xmlns="http://java.sun.com/xml/ns/javaee"
              xsi:schemaLocation="http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd">
     
         <display-name>oidc-sample-app</display-name>
     
         <filter>
             <filter-name>OIDCAgentFilter</filter-name>
             <filter-class>io.asgardeo.tomcat.oidc.agent.OIDCAgentFilter</filter-class>
         </filter>
         <filter-mapping>
             <filter-name>OIDCAgentFilter</filter-name>
             <url-pattern>/logout</url-pattern>
         </filter-mapping>
         <filter-mapping>
             <filter-name>OIDCAgentFilter</filter-name>
             <url-pattern>/oauth2client</url-pattern>
         </filter-mapping>
         <filter-mapping>
             <filter-name>OIDCAgentFilter</filter-name>
             <url-pattern>*.jsp</url-pattern>
         </filter-mapping>
         <filter-mapping>
             <filter-name>OIDCAgentFilter</filter-name>
             <url-pattern>*.html</url-pattern>
         </filter-mapping>
     
         <listener>
             <listener-class>io.asgardeo.tomcat.oidc.agent.SSOAgentContextEventListener</listener-class>
         </listener>
         <context-param>
             <param-name>app-property-file</param-name>
             <param-value>oidc-sample-app.properties</param-value>
         </context-param>
     
         <listener>
             <listener-class>io.asgardeo.tomcat.oidc.agent.JKSLoader</listener-class>
         </listener>
         <context-param>
             <param-name>jks-property-file</param-name>
             <param-value>jks.properties</param-value>
         </context-param>
     </web-app>
   ```
### Enable login    

Next, in the index.html, add a login button which we would use to forward the user to the secured page.
      ```html
        <form action="home.jsp" method="post">
            <div class="element-padding">
                <input style="height: 30px; width: 60px" type="submit" value="log in">
            </div>
        </form>
      ```

### Enable logout

Add the following snippet to enable logout from the secured page.

`<a href='logout'>Logout</a>`
      
   This has to match the value for the `logoutURL` property in the oidc-sample-app.properties file.

### Retrieving user attributes

1. The web app needs to be configured to read the attributes sent from Asgardeo upon successful
 authentication. In the oidc-sample-app, we would customize the home.jsp file as follows to retrieve the user attributes.
 
 First, we would need the following imports to be added to the home.jsp file.
 
       <%@page import="java.util.HashMap" %>
       <%@page import="java.util.Map" %>
       <%@ page import="io.asgardeo.java.oidc.sdk.bean.User" %>
       <%@ page import="io.asgardeo.java.oidc.sdk.bean.SessionContext" %>
       <%@ page import="io.asgardeo.java.oidc.sdk.SSOAgentConstants" %>
        
Next, by adding the following snippets, we would be able to retrieve the user claims as provided by the Identity Provider.
     
 ```java
        <%
            final HttpSession currentSession = request.getSession(false);
            final SessionContext sessionContext = (SessionContext)
                    currentSession.getAttribute(SSOAgentConstants.SESSION_CONTEXT);
            final String idToken = sessionContext.getIdToken().getParsedString();
            
                String name = null;
                Map<String, Object> customClaimValueMap = new HashMap<>();
                
                if (idToken != null) {
                    try {
                        final User user = sessionContext.getUser();
                        customClaimValueMap = user.getAttributes();
                        name = user.getSubject();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
        %>
 ```
     
    
      
2. Then, we would use the `customClaimValueMap` in the **<APP_HOME>/home.jsp** to display the user attributes via a 
table:

      ```html
           <table>
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
      ```
After the above configurations, your app would be able to try out the authentication, logout and attribute 
retrieval flows with OpenID Connect.
   