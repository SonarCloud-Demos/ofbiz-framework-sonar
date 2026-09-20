#!/bin/sh

set -eu

# The OFBiz image persists these markers separately from the environment-driven configuration.
# Remove only the markers so its own entrypoint reapplies database and external URL settings.
rm -f \
    /ofbiz/runtime/container_state/config_applied \
    /ofbiz/runtime/container_state/db_config_applied

exec /ofbiz/docker-entrypoint.sh "$@"
