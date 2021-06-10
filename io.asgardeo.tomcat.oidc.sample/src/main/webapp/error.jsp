<%--
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
  --%>

<%@ page import="io.asgardeo.java.oidc.sdk.exception.SSOAgentException" %>
<%@ page import="io.asgardeo.java.oidc.sdk.SSOAgentConstants" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>An error has occurred</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="stylesheet" type="text/css" href="theme.css">
</head>
<%
    SSOAgentException exception = (SSOAgentException) request.getAttribute(SSOAgentConstants.AGENT_EXCEPTION);
%>
<body>
    <div class="ui two column centered grid">
        <div class="column center aligned">
            <img src="images/logo-dark.svg" class="logo-image">
        </div>
        <div class="container">
            <div class="header-title">
                <h1>
                    Java-Based OIDC Authentication Sample<br>
                    (OIDC - Authorization Code Grant)
                </h1>
            </div>
            <div class="content">
                <h2>
                    An error has occurred!
                </h2>
                <h3>
                    <%=exception.getMessage()%>
                </h3>
            </div>
            <form action="index.html" method="post">
                <div class="element-padding">
                    <button class="btn primary" type="submit">Back</button>
                </div>
            </form>
        </div>
        <img src="images/footer.png" class="footer-image">
    </div>

    <script src="https://code.jquery.com/jquery-3.5.1.min.js" integrity="sha256-9/aliU8dGd2tb6OSsuzixeV4y/faTqgFtohetphbbj0=" crossorigin="anonymous"></script>

</body>
</html>
