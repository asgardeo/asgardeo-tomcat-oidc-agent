
## Integrating OIDC into your Existing Webapp 

### Getting started
Throughout this section we will refer to the existing web application as oidc-oidc-sample-app.

 
#### Prerequisites
1. [Apache Tomcat](http://tomcat.apache.org/tomcat-8.5-doc/) 8.x or higher

These instructions will guide you on integrating OIDC into your Java application with the Asgardio OIDC SDK.
This allows the developers to turn a Java application into a SP (Service Provider) that can be connected to an IdP
 (Identity Provider) which can then be secured with OIDC.

### Configuring the web app

The structure of the oidc-sample-app we are configuring would be as follows:

<img width="326" alt="structure" src="https://user-images.githubusercontent.com/25428696/91556626-aa2db880-e950-11ea-9203-72d2a68d4148.png">

1. Download the `lib.zip` from the [latest release](https://github.com/asgardio/asgardio-tomcat-oidc-agent/releases
/latest). [TODO]
   1. Extract the downloaded `lib.zip` file to the `<APP_HOME>/WEB-INF` directory. (If you already have a `lib` folder in
    your web app, merge the content of the downloaded `lib.zip` file into the existing `lib` folder.)

2. Before the web.xml configurations, we will look at adding the resources files.
   In the oidc-sample-app, create a file named oidc-sample-app.properties in the `<APP_HOME>/WEB-INF/classes` directory. The 
   oidc-sample-app.properties file contains properties similar to the following:

      ```
        consumerKey=<OAuth Client Key>
        consumerSecret=<OAuth Client Secret>
        skipURIs=/oidc-sample-app/index.html
        indexPage=
        errorPage=
        logoutURL=logout
        callBackURL=http://localhost:8080/oidc-sample-app/oauth2client
        scope=openid,internal_application_mgt_view
        #grantType=code
        authorizeEndpoint=https://localhost:9443/oauth2/authorize
        logoutEndpoint=https://localhost:9443/oidc/logout
        #sessionIFrameEndpoint=https://localhost:9443/oidc/checksession
        tokenEndpoint=https://localhost:9443/oauth2/token
        issuer=https://localhost:9443/oauth2/token
        jwksEndpoint=https://localhost:9443/oauth2/jwks
        postLogoutRedirectURI=http://localhost:8080/oidc-sample-app/index.html
        trustedAudience=http://localhost:8080/sample-app
        signatureAlgorithm=RS256
      ```
   These properties are required for the OIDC SDK to communicate with the WSO2 Identity Server.

3. Next, we need to find and set JKS properties required for IS server communication.  For that, create a file named jks
   .properties in the resources directory. The content of the jks.properties file should be similar to:
   
   ```
   keystorename=wso2carbon.jks
   keystorepassword=wso2carbon
   ```


4. Finally, copy and paste the following configurations to the `<APP_HOME>/WEB-INF/web.xml` file. 

      ```xml
      <?xml version="1.0" encoding="UTF-8"?>
        <web-app xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" id="SampleApp"
                 version="2.5"
                 xmlns="http://java.sun.com/xml/ns/javaee"
                 xsi:schemaLocation="http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd">
        
            <display-name>oidc-sample-app</display-name>
        
            <filter>
                <filter-name>OIDCAgentFilter</filter-name>
                <filter-class>io.asgardio.tomcat.oidc.agent.OIDCAgentFilter</filter-class>
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
### Enable login    
1. Next, the webapp itself has two pages, index.html and home.jsp, and a web.xml file.
The index.html contains a login button which we would use to forward the user to the secured page.
      ```html
        <form action="home.jsp" method="post">
            <div class="element-padding">
                <input style="height: 30px; width: 60px" type="submit" value="log in">
            </div>
        </form>
      ```
The home.jsp page is a page which we want to secure i.e. in case there are no active sessions, 
the http://localhost.com:8080/oidc-sample-app/home.jsp should not be accessible. 
In the sample we are using, if there is no active session in place, we would redirect the user for authentication. 

### Enable logout
1. In the home.jsp, there is a logout link which will be used to create a SLO request.

      `<a href='logout'>Logout</a>`
      
   This has to match the value for the `logoutURL` property in the oidc-sample-app.properties file.

### Retrieving user attributes

1. The web app needs to be configured to read the attributes sent from the Identity Server upon successful
 authentication. In the oidc-sample-app, we would customize the home.jsp file as follows to retrieve the user attributes.
 
 First, we would need the following imports to be added to the home.jsp file.
 
       <%@page import="java.util.HashMap" %>
       <%@page import="java.util.Map" %>
       <%@ page import="io.asgardio.java.oidc.sdk.bean.User" %>
       <%@ page import="io.asgardio.java.oidc.sdk.bean.SessionContext" %>
       <%@ page import="io.asgardio.java.oidc.sdk.SSOAgentConstants" %>
       
Next, by adding the following snippets, we would be able to retrieve the user claims as provided by the Identity Provider.

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
