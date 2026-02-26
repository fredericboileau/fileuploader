#!/bin/bash
set -e

KEYCLOAK_URL="http://localhost:8180"
REALM="filevault"
CLIENT_ID="filevault-app"
CLIENT_SECRET="filevault-secret"
REDIRECT_URI="http://localhost:8080/login/oauth2/code/keycloak"

echo "Getting admin token..."
TOKEN=$(curl -sf -X POST "$KEYCLOAK_URL/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=admin&password=admin&grant_type=password&client_id=admin-cli" |
  python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])")

echo "Creating realm '$REALM'..."
curl -sf -X POST "$KEYCLOAK_URL/admin/realms" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"realm\": \"$REALM\", \"enabled\": true}"

echo "Creating client '$CLIENT_ID'..."
curl -sf -X POST "$KEYCLOAK_URL/admin/realms/$REALM/clients" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"clientId\": \"$CLIENT_ID\",
    \"enabled\": true,
    \"publicClient\": false,
    \"standardFlowEnabled\": true,
    \"secret\": \"$CLIENT_SECRET\",
    \"redirectUris\": [\"$REDIRECT_URI\"],
    \"postLogoutRedirectUris\": [\"http://localhost:8080\"],
    \"webOrigins\": [\"http://localhost:8080\"]
  }"

echo "Client secret is: $CLIENT_SECRET"

echo ""
echo "Creating test user 'alice'..."
curl -sf -X POST "$KEYCLOAK_URL/admin/realms/$REALM/users" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"username\": \"alice\",
    \"enabled\": true,
    \"credentials\": [{\"type\": \"password\", \"value\": \"alice\", \"temporary\": false}]
  }"

echo "Creating test user 'bob'..."
curl -sf -X POST "$KEYCLOAK_URL/admin/realms/$REALM/users" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"username\": \"bob\",
    \"enabled\": true,
    \"credentials\": [{\"type\": \"password\", \"value\": \"bob\", \"temporary\": false}]
  }"

echo ""
echo "Done. Realm '$REALM' is ready with users alice/alice and bob/bob."
