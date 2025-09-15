
https://www.appsdeveloperblog.com/keycloak-authorization-code-grant-example/
http://localhost:8180/auth/realms/master/protocol/openid-connect/auth?client_id=box&response_type=code&state=fj8o3n7bdy1op5&redirect_uri=http%3A%2F%2Flocalhost%3A8080%2Ftest-auth
~~http://localhost:8180/auth/realms/master/p**rotocol/openid-connect/auth?client_id=box&response_type=code&state=fj8o3n7bdy1op5&redirect_uri=http://localhost:8080/test-auth~~


curl --location --request POST 'http://localhost:8180/auth/realms/master/protocol/openid-connect/token' \
--header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode 'grant_type=authorization_code' \
--data-urlencode 'client_id=box' \
--data-urlencode 'client_secret=ztoNKqtFUNvPbNIxTXk9kVWZR2nGtm9J' \
--data-urlencode 'code=48289e84-578f-45a9-8c9f-bc49fe336e3b.57b36158-2ad7-4614-80bf-3aa037ab38fe.2d13c739-13d5-4655-877f-56944b38e55e' \
--data-urlencode 'redirect_uri=http://localhost:8080/test-auth'