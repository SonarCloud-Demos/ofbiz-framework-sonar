#!/bin/sh
set -eu

gateway_url="${MODERN_GATEWAY_URL:-http://localhost:8081}"
cookie_jar="$(mktemp)"
headers="$(mktemp)"
trap 'rm -f "${cookie_jar}" "${headers}"' EXIT

anonymous_status="$(curl --silent --output /dev/null --write-out '%{http_code}' \
  "${gateway_url}/api/catalog/v1/products?query=Gizmo")"
[ "${anonymous_status}" = 401 ]

session="$(curl --fail --silent --show-error "${gateway_url}/auth/session")"
printf '%s' "${session}" | grep -q '"authenticated":0'

curl --fail --silent --show-error --cookie-jar "${cookie_jar}" \
  --request POST "${gateway_url}/auth/local/login" | grep -q '"authenticated":true'

api_response="$(curl --fail --silent --show-error --cookie "${cookie_jar}" \
  "${gateway_url}/api/catalog/v1/products?query=Gizmo")"
printf '%s' "${api_response}" | grep -q '"id":"GZ-1000"'

page="$(curl --fail --silent --show-error --dump-header "${headers}" "${gateway_url}/modern/catalog/products")"
printf '%s' "${page}" | grep -q 'data-runtime="modern"'
printf '%s' "${page}" | grep -q 'aria-label="Modern implementation"'
grep -qi '^Content-Security-Policy:' "${headers}"

curl --fail --silent --show-error --cookie "${cookie_jar}" --cookie-jar "${cookie_jar}" \
  --request POST "${gateway_url}/auth/logout" | grep -q '"authenticated":false'
logged_out_status="$(curl --silent --output /dev/null --write-out '%{http_code}' --cookie "${cookie_jar}" \
  "${gateway_url}/api/catalog/v1/products?query=Gizmo")"
[ "${logged_out_status}" = 401 ]

printf '%s\n' 'Phase 4 local authentication and routing smoke test passed.'
