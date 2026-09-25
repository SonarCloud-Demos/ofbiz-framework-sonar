#!/bin/sh
set -eu

secrets_file=local-dev/.env
umask 077
temporary_file=$(mktemp "${TMPDIR:-/tmp}/ofbiz-modern-env.XXXXXX")
trap 'rm -f "$temporary_file"' EXIT HUP INT TERM
if [ -f "$secrets_file" ]; then
  cp "$secrets_file" "$temporary_file"
fi
if ! grep -q '^REFERENCE_DATABASE_PASSWORD=' "$temporary_file"; then
  printf 'REFERENCE_DATABASE_PASSWORD=%s\n' "$(openssl rand -hex 24)" >> "$temporary_file"
fi
if ! grep -q '^BFF_KEYSTORE_PASSWORD=' "$temporary_file"; then
    printf 'BFF_KEYSTORE_PASSWORD=%s\n' "$(openssl rand -hex 24)" >> "$temporary_file"
fi
if ! grep -q '^LOCAL_AUTH_PASSWORD=' "$temporary_file"; then
  printf 'LOCAL_AUTH_PASSWORD=%s\n' "$(openssl rand -base64 24 | tr -d '\n')" >> "$temporary_file"
fi
if ! grep -q '^LEGACY_CATALOG_SYNC_TOKEN=' "$temporary_file"; then
  printf 'LEGACY_CATALOG_SYNC_TOKEN=%s\n' "$(openssl rand -hex 32)" >> "$temporary_file"
fi
if ! grep -q '^CATALOG_DATABASE_PASSWORD=' "$temporary_file"; then
  printf 'CATALOG_DATABASE_PASSWORD=%s\n' "$(openssl rand -hex 24)" >> "$temporary_file"
fi
mv "$temporary_file" "$secrets_file"
trap - EXIT HUP INT TERM
