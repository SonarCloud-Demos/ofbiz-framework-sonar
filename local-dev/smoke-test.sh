#!/bin/sh
set -eu

modern_origin=${MODERN_ORIGIN:-https://localhost:8444}
certificate=${MODERN_CA_CERTIFICATE:-local-dev/certs/modern-local.pem}
correlation_id=phase1-smoke
set -a
. local-dev/.env
set +a
credentials=modern-user:$LOCAL_AUTH_PASSWORD

shell=$(curl --user "$credentials" --cacert "$certificate" --fail --silent --show-error "$modern_origin/")
printf '%s' "$shell" | grep -q 'data-runtime="modern"'
printf '%s' "$shell" | grep -q 'Modern experience'

headers_file=$(mktemp)
body_file=$(mktemp)
trap 'rm -f "$headers_file" "$body_file"' EXIT HUP INT TERM

curl --fail --silent --show-error \
  --cacert "$certificate" \
  --user "$credentials" \
  --header "X-Correlation-ID: $correlation_id" \
  --dump-header "$headers_file" \
  --output "$body_file" \
  "$modern_origin/api/reference"

grep -qi "^X-Correlation-ID: $correlation_id" "$headers_file"
grep -Eqi '^traceparent: 00-[0-9a-f]{32}-[0-9a-f]{16}-01' "$headers_file"
grep -q '"service":"reference-service"' "$body_file"

curl --fail --silent --show-error \
  --cacert "$certificate" \
  --user "$credentials" \
  "$modern_origin/api/legacy/health" | grep -q '"runtime":"legacy","reachable":true'

curl --fail --silent --show-error \
  --cacert "$certificate" \
  --user "$credentials" \
  "$modern_origin/route-manifest.json" | grep -q '"default":"legacy"'

curl --fail --silent --show-error \
  --cacert "$certificate" \
  --user "$credentials" \
  "$modern_origin/modern/profile" | grep -q '"route":"modern"'
printf '%s\n' 'Phase 3 local shell/BFF/identity-bridge smoke test passed.'
