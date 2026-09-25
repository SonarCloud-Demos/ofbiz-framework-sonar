#!/bin/sh
set -eu

modern_origin=${MODERN_ORIGIN:-https://localhost:8444}
certificate=${MODERN_CA_CERTIFICATE:-local-dev/certs/modern-local.pem}
correlation_id=phase1-smoke
set -a
. local-dev/.env
set +a
credentials=modern-user:$LOCAL_AUTH_PASSWORD
legacy_origin=${LEGACY_ORIGIN:-http://localhost:8080}

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

curl --fail --silent --show-error \
  --cacert "$certificate" \
  --user "$credentials" \
  "$modern_origin/api/catalog/products?q=gizmo" | grep -q '"id":"GZ-1000"'

if curl --fail --silent --output /dev/null \
  "$legacy_origin/catalog/control/modernCatalogSnapshot"; then
  printf '%s\n' 'Legacy catalog snapshot unexpectedly accepted a request without its sync token.' >&2
  exit 1
fi
curl --fail --silent --show-error \
  --header "X-Modern-Catalog-Token: $LEGACY_CATALOG_SYNC_TOKEN" \
  "$legacy_origin/catalog/control/modernCatalogSnapshot" | grep -q '|'
if curl --fail --silent --output /dev/null \
  "$legacy_origin/catalog/control/modernCatalogChanges?after=0"; then
  printf '%s\n' 'Legacy catalog change feed unexpectedly accepted a request without its sync token.' >&2
  exit 1
fi

modern_catalog=$(curl --fail --silent --show-error \
  --cacert "$certificate" \
  --user "$credentials" \
  "$modern_origin/api/catalog/products")
printf '%s' "$modern_catalog" | grep -q '"id":"GZ-1000"'
if printf '%s' "$modern_catalog" | grep -q '"id":"WG-1112"'; then
  printf '%s\n' 'Modern catalog returned its fallback fixture instead of live OFBiz data.' >&2
  exit 1
fi
catalog_state=$(docker compose --env-file local-dev/.env -f local-dev/compose.yaml --profile legacy \
  exec --no-TTY catalog-db psql --username catalog --dbname catalog --tuples-only --no-align \
  --command "SELECT (SELECT count(*) FROM catalog_product) > 0, \
                    (SELECT count(*) FROM catalog_change) > 0, \
                    (SELECT count(*) FROM catalog_sync_state) = 1, \
                    (SELECT source_initialized FROM catalog_sync_state WHERE singleton)")
printf '%s' "$catalog_state" | grep -q '^t|t|t|t$'
printf '%s\n' 'Phase 4 local catalog read slice smoke test passed.'
