#!/bin/sh
set -eu

gateway_url="${MODERN_GATEWAY_URL:-http://localhost:8081}"
api_response="$(curl --fail --silent --show-error "${gateway_url}/api/catalog/v1/products?query=Gizmo")"
printf '%s' "${api_response}" | grep -q '"id":"GZ-1000"'

headers="$(mktemp)"
trap 'rm -f "${headers}"' EXIT
page="$(curl --fail --silent --show-error --dump-header "${headers}" "${gateway_url}/modern/catalog/products")"
printf '%s' "${page}" | grep -q 'data-runtime="modern"'
printf '%s' "${page}" | grep -q 'aria-label="Modern implementation"'
grep -qi '^Content-Security-Policy:' "${headers}"
printf '%s\n' 'Modern golden-path smoke test passed.'
