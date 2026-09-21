#!/bin/sh
set -eu
script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
mkdir -p "$script_dir/certs"
openssl req -x509 -newkey rsa:2048 -nodes -days 30 -subj '/CN=localhost' \
  -addext 'subjectAltName=DNS:localhost,IP:127.0.0.1' \
  -keyout "$script_dir/certs/local.key" -out "$script_dir/certs/local.crt"
