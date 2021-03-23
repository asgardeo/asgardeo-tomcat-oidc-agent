# Asgardeo Tomcat OIDC Agent


[![Build Status](https://img.shields.io/jenkins/build?jobUrl=https%3A%2F%2Fwso2.org%2Fjenkins%2Fjob%2Fasgardeo%2Fjob%2Fasgardeo-tomcat-oidc-agent%2F&style=flat)](https://wso2.org/jenkins/job/asgardeo/job/asgardeo-tomcat-oidc-agent/)
[![Stackoverflow](https://img.shields.io/badge/Ask%20for%20help%20on-Stackoverflow-orange)](https://stackoverflow.com/questions/tagged/wso2is)
[![Join the chat at https://join.slack.com/t/wso2is/shared_invite/enQtNzk0MTI1OTg5NjM1LTllODZiMTYzMmY0YzljYjdhZGExZWVkZDUxOWVjZDJkZGIzNTE1NDllYWFhM2MyOGFjMDlkYzJjODJhOWQ4YjE](https://img.shields.io/badge/Join%20us%20on-Slack-%23e01563.svg)](https://join.slack.com/t/wso2is/shared_invite/enQtNzk0MTI1OTg5NjM1LTllODZiMTYzMmY0YzljYjdhZGExZWVkZDUxOWVjZDJkZGIzNTE1NDllYWFhM2MyOGFjMDlkYzJjODJhOWQ4YjE)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://github.com/wso2/product-is/blob/master/LICENSE)
[![Twitter](https://img.shields.io/twitter/follow/wso2.svg?style=social&label=Follow)](https://twitter.com/intent/follow?screen_name=wso2)
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
  * [Configuring Identity Server](#configuring-identity-server)
  * [Configuring the sample](#configuring-the-sample)
  * [Running the sample](#running-the-sample)

### Prerequisites
1. WSO2 Identity Server and its [prerequisites](https://is.docs.wso2.com/en/next/setup/installing-the-product/).
2. [Apache Tomcat](https://tomcat.apache.org/tomcat-9.0-doc/) 8.x or 9.x.
> **NOTE**  
> If you are using Apache Tomcat 10 or a later version, use [this conversion](https://tomcat.apache.org/download-migration.cgi) tool to change the namespaces of the web application. This is necessary because of the namespace changes introduced in Tomcat 10.

### Configuring Identity Server
Here we are using WSO2 Identity Server as the OpenID Provider. The sample can be configured with any other preferred
 OpenID Provider as well.
 
1. Start the WSO2 IS. 
2. Access WSO2 IS management console and create a service provider (ex:- oidc-sample-app)
   
   For the service provider, configure Oauth/OpenID Connect under Inbound Authentication Configuration. In this
    configuration,
   use following parameters and options,
     
       Callback URL - regexp=(http://localhost:8080/oidc-sample-app/oauth2client|http://localhost:8080/oidc-sample-app/index.html)


   Keep the other default settings as it is and save the configuration.
   
   Next, expand the [Claim Configuration](https://is.docs.wso2.com/en/latest/learn/configuring-claims-for-a-service-provider/#configuring-claims-for-a-service-provider) section. In this configuration, Set the following config and add the claims you 
   need to retrieve (ex: http://wso2.org/claims/lastname) from the web app.
   
       Select Claim mapping Dialect - Use Local Claim Dialect
       
   See the example claim config below.
   ![Claim Config](https://user-images.githubusercontent.com/15249242/90488235-38d45580-e159-11ea-8beb-52d6b5c35034.png)

### Configuring the sample
1. Download the [oidc-sample-app.war](https://github.com/asgardeo/asgardeo-tomcat-oidc-agent/releases/download/v0.1.4/oidc-sample-app.war).
2. Update the `consumerKey` and `consumerSecret` values in the `oidc-sample-app.properties` file in `<APP_HOME>/WEB-INF/classes` 
directory with the `OAuth Client Key` and `OAuth Client Secret` values obtained after configuring an SP in
Identity Server. Set `indexPage` value to `/oidc-sample-app/index.html`.
2. Deploy the application, `oidc-sample-app.war` using Apache Tomcat.

### Running the sample
1. Try out the application by accessing the `http://localhost:8080/oidc-sample-app/index.html`.
 
![Recordit GIF](https://user-images.githubusercontent.com/25479743/99786157-28f75380-2b44-11eb-97e1-4f505841ac26.gif)

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

The web app needs to be configured to read the attributes sent from the Identity Server upon successful
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
    <version>0.1.9</version>
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

