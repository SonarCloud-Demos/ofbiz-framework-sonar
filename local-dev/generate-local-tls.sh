#!/bin/sh
set -eu

repository_root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
certificate_directory="$repository_root/local-dev/certs"

umask 077
mkdir -p "$certificate_directory"
openssl req -x509 -nodes -newkey rsa:2048 -days 30 \
    -keyout "$certificate_directory/local.key" \
    -out "$certificate_directory/local.crt" \
    -subj '/CN=localhost' \
    -addext 'subjectAltName=DNS:localhost'

printf 'Created %s and %s\n' \
    "$certificate_directory/local.crt" \
    "$certificate_directory/local.key"
