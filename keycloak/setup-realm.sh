#!/bin/bash

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
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$KEYCLOAK_URL/admin/realms" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"realm\": \"$REALM\", \"enabled\": true}")
if [ "$STATUS" = "201" ]; then
  echo "Realm created."
elif [ "$STATUS" = "409" ]; then
  echo "Realm already exists, skipping."
else
  echo "Unexpected status $STATUS creating realm, aborting." && exit 1
fi

echo "Creating client '$CLIENT_ID'..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$KEYCLOAK_URL/admin/realms/$REALM/clients" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"clientId\": \"$CLIENT_ID\",
    \"enabled\": true,
    \"publicClient\": false,
    \"standardFlowEnabled\": true,
    \"secret\": \"$CLIENT_SECRET\",
    \"redirectUris\": [\"$REDIRECT_URI\"],
    \"webOrigins\": [\"http://localhost:8080\"],
    \"attributes\": {\"post.logout.redirect.uris\": \"http://localhost:8080\"}
  }")
if [ "$STATUS" = "201" ]; then
  echo "Client created. Secret is: $CLIENT_SECRET"
elif [ "$STATUS" = "409" ]; then
  echo "Client already exists, skipping."
else
  echo "Unexpected status $STATUS creating client, aborting." && exit 1
fi

create_user() {
  local USERNAME=$1
  local PASSWORD=$2
  echo "Creating user '$USERNAME'..."
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$KEYCLOAK_URL/admin/realms/$REALM/users" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "{
      \"username\": \"$USERNAME\",
      \"enabled\": true,
      \"credentials\": [{\"type\": \"password\", \"value\": \"$PASSWORD\", \"temporary\": false}]
    }")
  if [ "$STATUS" = "201" ]; then
    echo "User '$USERNAME' created."
  elif [ "$STATUS" = "409" ]; then
    echo "User '$USERNAME' already exists, skipping."
  else
    echo "Unexpected status $STATUS creating user '$USERNAME', aborting." && exit 1
  fi
}

create_user "alice" "alice"
create_user "bob" "bob"

echo ""
echo "Done. Realm '$REALM' is ready with users alice/alice and bob/bob."
