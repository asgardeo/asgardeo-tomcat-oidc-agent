# Asgardeo Tomcat OIDC Agent


[![Build Status](https://img.shields.io/jenkins/build?jobUrl=https%3A%2F%2Fwso2.org%2Fjenkins%2Fjob%2Fasgardeo%2Fjob%2Fasgardeo-tomcat-oidc-agent%2F&style=flat)](https://wso2.org/jenkins/job/asgardeo/job/asgardeo-tomcat-oidc-agent/)
[![Stackoverflow](https://img.shields.io/badge/Ask%20for%20help%20on-Stackoverflow-orange)](https://stackoverflow.com/questions/tagged/asgardeo)
[![Join the chat at https://join.slack.com/t/wso2is/shared_invite/enQtNzk0MTI1OTg5NjM1LTllODZiMTYzMmY0YzljYjdhZGExZWVkZDUxOWVjZDJkZGIzNTE1NDllYWFhM2MyOGFjMDlkYzJjODJhOWQ4YjE](https://img.shields.io/badge/Join%20us%20on-Slack-%23e01563.svg)](https://join.slack.com/t/wso2is/shared_invite/enQtNzk0MTI1OTg5NjM1LTllODZiMTYzMmY0YzljYjdhZGExZWVkZDUxOWVjZDJkZGIzNTE1NDllYWFhM2MyOGFjMDlkYzJjODJhOWQ4YjE)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://github.com/asgardeo/asgardeo-tomcat-oidc-agent/blob/master/LICENSE)
[![Twitter](https://img.shields.io/twitter/follow/wso2.svg?style=social&label=Follow)](https://twitter.com/intent/follow?screen_name=asgardeo)
---

The Asgardeo Tomcat OIDC Agent enables you to add OIDC-based login, logout to your Apache Tomcat web apps with
 minimum hassle.


- [Getting Started](#getting-started)
- [How it works](#how-it-works)
- [Integrating Asgardeo Tomcat OIDC Agent](#integrating-asgardeo-tomcat-oidc-agent)
  * [To your existing webapp](#to-your-existing-webapp)
  * [To your Java source project](#to-your-java-source-project)
- [Building from the source](#building-from-the-source)
- [Contributing](#contributing)
  * [Reporting issues](#reporting-issues)
- [License](#license)

## Getting started

You can experience the capabilities of Asgardeo Tomcat OIDC Agent by following this small guide which contains main
 sections listed below.

  * [Prerequisites](#prerequisites)
  * [Create an Application in Asgardeo](#1-create-an-application-in-asgardeo)
  * [Running the sample apps](#2-running-the-sample-apps)

### Prerequisites
- [Apache Tomcat](https://tomcat.apache.org/tomcat-9.0-doc/) 8.x or 9.x.
> **NOTE**  
> If you are using Apache Tomcat 10 or a later version, use [this conversion](https://tomcat.apache.org/download-migration.cgi) tool to change the namespaces of the web application. This is necessary because of the namespace changes introduced in Tomcat 10.

### 1. Create an Application in Asgardeo
Here we are using Asgardeo as the OpenID Provider.

1. Navigate to [**Asgardeo Console**](https://console.asgardeo.io/login) and click on **Applications** under **Develop** tab
   
2. Click on **New Application** and then **Standard Based Application**.
   
3. Select OIDC from the selection and enter any name as the name of the app and add the redirect URL(s).
   
4. Click on Register. You will be navigated to management page of the created application.
   
5. Add `https://localhost:8080` (or whichever the URL your app is hosted on) to **Allowed Origins** under **Protocol** tab and check **Public client** option.
   
6. Click on **Update** at the bottom.

### 2. Running the sample apps
1. Download the [oidc-sample-app.war](https://github.com/asgardeo/asgardeo-tomcat-oidc-agent/releases/download/v0.1.4/oidc-sample-app.war).

2. Deploy the application, `oidc-sample-app.war` using Apache Tomcat.

3. Update the `consumerKey`, `consumerSecret`, `callBackURL`, `authorizeEndpoint`, `logoutEndpoint`, `tokenEndpoint`, `issuer` and `jwksEndpoint` values in the `oidc-sample-app.properties` file in `<APP_HOME>/WEB-INF/classes` directory using the value in the Asgardeo Console. These values can be found in the the **Info tab** of the Management settings of the OIDC application you created in Asgardeo.<img alt="Screenshot 2021-10-12 at 19 21 55" src="https://user-images.githubusercontent.com/42619922/136969300-1b811573-d80b-4c31-b028-95f983765643.png">
5. Restart the Tomcat server to allow the changes.
6. Try out the application by accessing the `http://localhost:8080/oidc-sample-app/index.html`.

![Screen Recording 2021-10-12 at 19 19 13](https://user-images.githubusercontent.com/42619922/136969578-5e5bbfa6-fe20-4fcb-bd77-6e0ffdb68e91.gif)

 


## How it works

This section contains a detailed walk-through on how the Asgardeo Tomcat OIDC Agent is handling key aspects of the
 web app.

  * [Classify secure resources, unsecured resources](#classify-secure-resources-unsecured-resources)
  * [Trigger authentication](#trigger-authentication)
  * [Retrieve user attributes](#retrieve-user-attributes)
  * [Trigger logout](#trigger-logout)
 
### Classify secure resources, unsecured resources
In the sample-app, we have two pages. A landing page (`index.html`) which we have not secured, and another 
page (`home.jsp`) which we have secured.

`indexPage` property of the oidc-sample-app.properties file in the `<APP_HOME>/WEB-INF/classes` directory is used to
 define the landing page of the webapp. This is considered as an unsecured page.
Also, once the logout is done, the user gets redirected to this same page.
Here we have set `<APP_HOME>/index.html` as the value of `indexPage` property.

    indexPage=/oidc-sample-app/index.html

By default, all the other pages are considered as secured pages. Hence `home.jsp` will be secured without any other configurations.

### Trigger authentication

In the **index.html** page of the oidc-sample-app, the login button would send a request to the **home.jsp** page.
  This request would engage the **OIDCAgentFilter** which is specified in the **web.xml** file in the
 `<APP_HOME>/WEB-INF/` directory. There, it would check if there is an authenticated session in place. If the session
  is authenticated, the request would be handled by the **HTTPSessionBasedOIDCProcessor** and would forward the user
   to the **home.jsp** page.
   
   In case the current session is not authenticated, the filter would initiate an authentication request and redirect
    the user for authentication. Upon successful authentication, the request would engage the 
    **HTTPSessionBasedOIDCProcessor** and the user would be redirected to the **home.jsp** page.

### Retrieve user attributes

The web app needs to be configured to read the attributes sent from Asgardeo upon successful
 authentication. In the oidc-sample-app, we would customize the home.jsp file as follows to retrieve the user
  attributes.
 
 ```
<%
    // Retrieve the current session.
    final HttpSession currentSession = request.getSession(false);

    // Logged in session context.
    final SessionContext sessionContext = (SessionContext)
            currentSession.getAttribute(SSOAgentConstants.SESSION_CONTEXT);

    // Logged in user.
    final User user = sessionContext.getUser();

    // Attributes of the logged in user.
    Map<String, Object> customClaimValueMap = user.getAttributes();
%>
```


### Trigger logout

In the **home.jsp** file, we have added the following to trigger a logout flow:

``<a href='logout'>Logout</a>``

Clicking on the logout link would trigger the logout flow engaging the same **OIDCAgentFilter** mentioned above.
After successful logout, the user would be redirected to the page configured via the `indexPage` property previously
 discussed.

## Integrating Asgardeo Tomcat OIDC Agent

Asgardeo Tomcat OIDC Agent can be integrated in to your applications in two different ways. 

It can be integrated to your java source project of the webapp when the web application is in development stage.

And, the Tomcat OIDC agent can be integrated into a pre-built webapp as well.

#### To your existing webapp

To integrate the Tomcat OIDC Agent into your pre-built webapps, follow the guide [here](docs/integrating_with_existing_webapp.md/#Integrating_OIDC_into_your_existing_Webapp).

#### To your Java source project

To integrate the Tomcat OIDC Agent into your java source project, follow the guide [here](docs/integrating_with_java_source_project.md/#Integrating_OIDC_into_your_java_source_project).


## Installing the Agent
### Maven
Install it as a maven dependency:
```
<dependency>
    <groupId>io.asgardeo.tomcat.oidc.agent</groupId>
    <artifactId>io.asgardeo.tomcat.oidc.agent</artifactId>
    <version>0.1.18</version>
</dependency>
```
### Building from the source

If you want to build **asgardeo-tomcat-oidc-agent** from the source code:

1. Install Java 8
2. Install Apache Maven 3.x.x (https://maven.apache.org/download.cgi#)
3. Get a clone or download the source from this repository (https://github.com/asgardeo/asgardeo-tomcat-oidc-agent.git)
4. Run the Maven command ``mvn clean install`` from the ``asgardeo-tomcat-oidc-agent`` directory.


## Contributing

Please read [Contributing to the Code Base](http://wso2.github.io/) for details on our code of conduct, and the
 process for submitting pull requests to us.
 
### Reporting Issues
We encourage you to report issues, improvements, and feature requests creating [git Issues](https://github.com/asgardeo/asgardeo-tomcat-oidc-agent/issues).

Important: Please be advised that security issues must be reported to security@wso2.com, not as GitHub issues, 
in order to reach the proper audience. We strongly advise following the WSO2 Security Vulnerability Reporting Guidelines
 when reporting the security issues.

## Versioning

For the versions available, see the [tags on this repository](https://github.com/asgardeo/asgardeo-tomcat-oidc-agent/tags). 

## Authors


See also the list of [contributors](https://github.com/asgardeo/asgardeo-tomcat-oidc-agent/graphs/contributors) who
 participated in this project.

## License

This project is licensed under the Apache License 2.0 under which WSO2 Carbon is distributed. See the [LICENSE
](LICENSE) file for details.

