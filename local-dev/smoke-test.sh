#!/bin/sh
set -eu

gateway_url="${MODERN_GATEWAY_URL:-http://localhost:8081}"
cookie_jar="$(mktemp)"
headers="$(mktemp)"
trap 'rm -f "${cookie_jar}" "${headers}"' EXIT

anonymous_status="$(curl --silent --output /dev/null --write-out '%{http_code}' \
  "${gateway_url}/api/catalog/v1/products?internalName=Gizmo")"
[ "${anonymous_status}" = 401 ]

session="$(curl --fail --silent --show-error "${gateway_url}/auth/session")"
printf '%s' "${session}" | grep -q '"authenticated":0'

curl --fail --silent --show-error --cookie-jar "${cookie_jar}" \
  --request POST "${gateway_url}/auth/local/login" | grep -q '"authenticated":true'

api_response="$(curl --fail --silent --show-error --cookie "${cookie_jar}" \
  "${gateway_url}/api/catalog/v1/products?internalName=Gizmo")"
printf '%s' "${api_response}" | grep -q '"productId":"GZ-1000"'
printf '%s' "${api_response}" | grep -Eq '"total":[1-9][0-9]*'

page="$(curl --fail --silent --show-error --dump-header "${headers}" "${gateway_url}/modern/catalog/products")"
printf '%s' "${page}" | grep -q 'data-runtime="mixed"'
printf '%s' "${page}" | grep -q 'aria-label="Mixed implementation"'
grep -qi '^Content-Security-Policy:' "${headers}"

curl --fail --silent --show-error --cookie "${cookie_jar}" --cookie-jar "${cookie_jar}" \
  --request POST "${gateway_url}/auth/local/pilot/modern" | grep -q '"catalogPilot":"modern"'
cohort_page="$(curl --fail --silent --show-error --cookie "${cookie_jar}" --dump-header "${headers}" \
  "${gateway_url}/catalog/control/FindProduct")"
printf '%s' "${cohort_page}" | grep -q 'data-runtime="mixed"'
grep -qi '^X-Catalog-Pilot-Route: modern_shell' "${headers}"

curl --fail --silent --show-error --cookie "${cookie_jar}" --cookie-jar "${cookie_jar}" \
  --request POST "${gateway_url}/auth/local/pilot/legacy" | grep -q '"catalogPilot":"legacy"'

if [ "${PHASE5_EXPECT_LEGACY:-false}" = true ]; then
  legacy_status="$(curl --silent --output /dev/null --write-out '%{http_code}' --cookie "${cookie_jar}" \
    "${gateway_url}/catalog/control/EditProduct?productId=GZ-1000")"
  case "${legacy_status}" in
    200|302|303|401) ;;
    *) printf 'Legacy transition returned HTTP %s\n' "${legacy_status}" >&2; exit 1 ;;
  esac
fi

curl --fail --silent --show-error --cookie "${cookie_jar}" --cookie-jar "${cookie_jar}" \
  --request POST "${gateway_url}/auth/logout" | grep -q '"authenticated":false'
logged_out_status="$(curl --silent --output /dev/null --write-out '%{http_code}' --cookie "${cookie_jar}" \
  "${gateway_url}/api/catalog/v1/products?internalName=Gizmo")"
[ "${logged_out_status}" = 401 ]

printf '%s\n' 'Phase 5 local catalog slice smoke test passed.'
