#!/bin/sh
set -eu

require_text() {
  pattern="$1"
  file="$2"
  if ! rg --quiet --fixed-strings -- "${pattern}" "${file}"; then
    printf 'Missing Phase 4 route evidence %s in %s\n' "${pattern}" "${file}" >&2
    exit 1
  fi
}

for route in /modern/catalog/products /api/catalog/v1/products /catalog/control/FindProduct; do
  require_text "${route}" architecture/route-ownership.yaml
  require_text "${route}" web/shell/route-manifest.json
done

require_text 'url_template        = "/v1/products"' infra/modules/platform/main.tf
require_text 'url_template        = "/control/FindProduct"' infra/modules/platform/main.tf
require_text 'CATALOG_SECURITY_MODE: local' local-dev/docker-compose.yml

if rg --quiet --fixed-strings -- 'OFBIZ_IDENTITY_BRIDGE_MODE: entra' local-dev/docker-compose.yml; then
  printf '%s\n' 'The Entra identity bridge must not be enabled in the local Compose stack.' >&2
  exit 1
fi

printf '%s\n' 'Phase 4 route and local-auth invariants passed.'
