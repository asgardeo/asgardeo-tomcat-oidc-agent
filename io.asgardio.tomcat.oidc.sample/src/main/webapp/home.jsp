<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html>
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

<%@page import="io.asgardio.java.oidc.sdk.SSOAgentConstants" %>
<%@page import="io.asgardio.java.oidc.sdk.bean.SessionContext" %>
<%@ page import="io.asgardio.java.oidc.sdk.bean.User" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.Map" %>

<%
    final HttpSession currentSession = request.getSession(false);
    final SessionContext sessionContext = (SessionContext)
            currentSession.getAttribute(SSOAgentConstants.SESSION_CONTEXT);
    final String idToken = sessionContext.getIdToken();
    
    String name = null;
    Map<String, Object> customClaimValueMap = new HashMap<>();
    
    if (idToken != null) {
        final User user = sessionContext.getUser();
        customClaimValueMap = user.getAttributes();
        name = user.getSubject();
    }
%>

<html>
<head>
    <title>Home</title>
    <style>
        html, body {
            height: 100%;
        }

        body {
            flex-direction: column;
            display: flex;
        }

        main {
            flex-shrink: 0;
        }

        main.center-segment {
            margin: auto;
            display: flex;
            align-items: center;
        }

        .element-padding {
            margin: auto;
            padding: 15px;
        }

        .center {
            margin-left: auto;
            margin-right: auto;
        }
    </style>
</head>
<body>
<main class="center-segment">
    <div style="text-align: center">
        <div class="element-padding">
            <h1>OIDC Sample App Home Page!</h1>
        </div>
        
        <div class="element-padding">
            <h1>Hi <%=name%>
            </h1>
        </div>
        
        <% if (!customClaimValueMap.isEmpty()) { %>
        <div class="element-padding">
            <div class="element-padding">
                <h3 align="center">User Details</h3>
            </div>
            <table class="center">
                <tbody>
                <% for (String claim : customClaimValueMap.keySet()) { %>
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
        
        <div class="element-padding">
            <a href='logout'>Logout</a>
        </div>
    </div>
</main>
</body>
</html>
