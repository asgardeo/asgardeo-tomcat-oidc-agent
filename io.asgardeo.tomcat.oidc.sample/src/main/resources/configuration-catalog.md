# Configuration Catalog

This document describes all the configuration parameters that are used in Asgardeo OIDC Agent.

## Required Parameters
### Consumer Key / Client ID
**Property Name:** `consumerKey`

**Description:** This refers to the client identifier assigned to the relying Party by the OpenID Provider during its
 registration with the OpenID Provider.
 
 **Sample:** 
 
```
 consumerKey=<OAuth Client Key>
```
 
### Consumer Secret / Client Secret
**Property Name:** `consumerSecret`

**Description:** The client secret as defined by OAuth 2.0 (RFC 6749), sections 2.3.1 and 3.2.1.

**Sample:** 

```
consumerSecret=<OAuth Client Secret>
```
 
 ### Callback URI / Redirect URI
**Property Name:** `callBackURL`
 
**Description:** This is the URL to which the response will be sent upon successful authentication. This URL
  MUST exactly match one of the callback URL values for the Client pre-registered at the OpenID Provider.

**Sample:** 

```
callBackURL=http://localhost:8080/oidc-sample-app/oauth2client
```

### Scope
 
**Property Name:** `scope`
 
**Description:** Scope parameter must contain the value 'openid'. It may have other scope values in comma-separated
  strings.

**Sample:** 

```
scope=openid,internal,manager
```

### Authorize Endpoint
**Property Name:** `authorizeEndpoint`
 
**Description:** Authorization Server's authorization endpoint. The Authorization Endpoint performs the authentication of
  the End-User.

**Sample:** 

```
authorizeEndpoint=https://localhost:9443/oauth2/authorize
```

### Token Endpoint
 
**Property Name:** `tokenEndpoint`
 
**Description:** Authorization Server's token endpoint. This is the endpoint where access tokens or ID tokens or
  refresh tokens are issued upon token requests.

**Sample:** 

```
tokenEndpoint=https://localhost:9443/oauth2/token
```

### Logout Endpoint
 
**Property Name:** `logoutEndpoint`
 
**Description:** The endpoint at the OpenID Provider that is the target of RP-Initiated Logout requests. The value MUST
  be a URL using the https scheme. 

**Sample:** 

```
logoutEndpoint=https://localhost:9443/oidc/logout
```

 ### Issuer
 
**Property Name:** `issuer`
 
**Description:** The Issuer Identifier for the OpenID Provider. The value MUST be a URL using the https scheme. 

**Sample:** 

```
issuer=https://localhost:9443/oauth2/token
```

### JWKS Endpoint
 
**Property Name:** `jwksEndpoint`
 
**Description:** The endpoint where the JWK sets are published. The JWK set is used for signature verification of the
  tokens of the issuer.

**Sample:** 

```
jwksEndpoint=https://localhost:9443/oauth2/jwks
```


### Logout URL

**Property Name:** `logoutURL`

**Description:** This is the URL that denotes the logout endpoint for the application.

**Sample:**

```
logoutURL=logout
```

## Conditional Parameters
### Trusted Audience
**Property Name:** `trustedAudience`
 
**Description:** In case there are multiple audiences for the ID token issued from the OpenID Provider, this parameter is
  mandatory. In such cases, the trustedAudience attribute must contain all the audience values that are sent in the
   ID Token `aud` parameter.

**Sample:** 

```
trustedAudience=http://localhost:8080/sample-app, https://localhost:8080/pet-app
```
### Signature Algorithm
**Property Name:** `signatureAlgorithm`
 
**Description:** This parameter is mandatory if the OpenID Provider uses a different algorithm for signing other than
  the default `RS256`. In such cases the signature algorithm MUST match the algorithm used by the OpenID Provider for
   signing the tokens.

**Sample:** 

```
signatureAlgorithm=EC256
```

## Optional Parameters

### Skip URIs

**Property Name:** `skipURIs`
 
**Description:** This parameter may include URIs that need not be secured. Multiple URIs can be set using comma separated
  values.

**Sample:** 

```
skipURIs=/oidc-sample-app/page1.jsp,/oidc-sample-app/page2.jsp
```

### Index Page

**Property Name:** `indexPage`
 
**Description:** This parameter may denote the URI for the landing page of the webapp.

**Sample:** 

```
indexPage=/oidc-sample-app/index.html
```
### Error Page

**Property Name:** `errorPage`
 
**Description:** This parameter may denote the URI for the error page of the webapp.

**Sample:** 

```
errorPage=/error.jsp
```

### Home Page

**Property Name:** `homePage`

**Description:** This may denote the URI for the home page of the webapp. This parameter can be configured to specify 
the page that the user would always be redirected to, after successful authentication.


**Sample:**

```
homePage=home.jsp
```

### Post Logout Redirect URI

**Property Name:** `postLogoutRedirectURI`

**Description:** This is the URI to which the user will be returned to upon successfully signing out from the OpenID Provider. If not configured, the callbackURL will be used instead.


**Sample:**

```
postLogoutRedirectURI=http://localhost:8080/oidc-sample-app/oauth2client
```

### HTTP Connect Timeout

**Property Name:** `httpConnectTimeout`

**Description:** This parameter denotes the timeout in milliseconds for establishing the connection to the OpenID Provider. 2000 ms (2 seconds) by default.

**Sample:**

```
httpConnectTimeout=2000
```

### HTTP Read Timeout

**Property Name:** `httpReadTimeout`

**Description:** This parameter denotes the timeout in milliseconds for reading the data received from the OpenID Provider. 2000 ms (2 seconds) by default.

**Sample:**

```
httpReadTimeout=2000
```

### HTTP Size Limit

**Property Name:** `httpSizeLimit`

**Description:** This parameter denotes the HTTP entity size limit in bytes. 51200 bytes (50 KBytes) by default.

**Sample:**

```
httpSizeLimit=51200
```

### State

**Property Name:** `state`

**Description:** This parameter denotes an opaque value which can be used to maintain the state between the request and the callback. This can also be used to prevent cross-site request forgery (CSRF) attacks. A random UUID is assigned by default.

### Additional Parameters for Authorize Endpoint

**Property Name:** `additionalParamsForAuthorizeEndpoint` 

**Description:** This parameter denotes the additional query parameters that required to be sent to the OpenID Provider.



