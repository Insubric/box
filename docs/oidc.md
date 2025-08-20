---
title: Open ID Connect
permalink: /oidc/
nav_order: 2
---

# Open ID Connect 

### Example Configuration

```hocon
oidc = [{
  provider_id = "google"
  name = "Google"
  logo = "https://example.com/logo.png"
  authorize_url = "https://accounts.google.com/o/oauth2/auth"
  token_url = "https://oauth2.googleapis.com/token"
  user_info_url = "https://www.googleapis.com/oauth2/v1/userinfo"
  scope = "openid email profile"
  client_id = "123456789012-abcdefghijklmnopqrstuvwxyz.apps.googleusercontent.com"
  client_secret = "abcdefghijklmnopqrstuvwxyz"
  db_role_claim = "role"
  app_user_claim = "sub"
}]
```

## Fields

### `provider_id`
- **Description**: A unique identifier for the OIDC provider.
- **Type**: `String`
- **Example**: `"google"`

### `name`
- **Description**: The name of the OIDC provider.
- **Type**: `String`
- **Example**: `"Google"`

### `logo`
- **Description**: The URL or path to the logo of the OIDC provider.
- **Type**: `String`
- **Example**: `"https://example.com/logo.png"`

### `authorize_url`
- **Description**: The URL used to initiate the authorization process with the OIDC provider.
- **Type**: `String`
- **Example**: `"https://accounts.google.com/o/oauth2/auth"`

### `token_url`
- **Description**: The URL used to obtain an access token from the OIDC provider.
- **Type**: `String`
- **Example**: `"https://oauth2.googleapis.com/token"`

### `user_info_url`
- **Description**: The URL used to retrieve user information from the OIDC provider.
- **Type**: `String`
- **Example**: `"https://www.googleapis.com/oauth2/v1/userinfo"`

### `scope`
- **Description**: The scope of the access request, specifying the level of access required.
- **Type**: `String`
- **Example**: `"openid email profile"`

### `client_id`
- **Description**: The client identifier issued to the application by the OIDC provider.
- **Type**: `String`
- **Example**: `"123456789012-abcdefghijklmnopqrstuvwxyz.apps.googleusercontent.com"`

### `client_secret`
- **Description**: The client secret issued to the application by the OIDC provider.
- **Type**: `String`
- **Example**: `"abcdefghijklmnopqrstuvwxyz"`

### `db_role_claim`
- **Description**: An optional claim that specifies the role of the user in the database.
- **Type**: `Option[String]`
- **Example**: `"role"`

### `app_user_claim`
- **Description**: An optional claim that specifies the user identifier in the application.
- **Type**: `Option[String]`
- **Example**: `"sub"`

