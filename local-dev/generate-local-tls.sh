#!/bin/sh
set -eu

local-dev/generate-local-secrets.sh
set -a
. local-dev/.env
set +a

certificate_directory=local-dev/certs
key_store=$certificate_directory/modern-local.p12
certificate=$certificate_directory/modern-local.pem
if [ -f "$key_store" ] && [ -f "$certificate" ]; then
  exit 0
fi

mkdir -p "$certificate_directory"
umask 077
keytool -genkeypair \
  -alias modern-local \
  -keyalg RSA \
  -keysize 3072 \
  -validity 30 \
  -dname 'CN=localhost, OU=Local Development, O=OFBiz, L=Local, ST=Local, C=CH' \
  -ext 'SAN=dns:localhost,ip:127.0.0.1' \
  -storetype PKCS12 \
  -keystore "$key_store" \
  -storepass "$BFF_KEYSTORE_PASSWORD" \
  -keypass "$BFF_KEYSTORE_PASSWORD" \
  -noprompt
keytool -exportcert \
  -rfc \
  -alias modern-local \
  -keystore "$key_store" \
  -storepass "$BFF_KEYSTORE_PASSWORD" \
  -file "$certificate"
