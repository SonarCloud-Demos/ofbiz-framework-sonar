# Phase 1 hybrid stack

Generate the local certificate once, then start the stack:

```sh
export NPM_CONFIG_USERCONFIG="$HOME/.npmrc"
export CORPORATE_CA_FILE="$HOME/.certs/Sonar-CloudFlare-Inspection-Cert.pem"
export GRADLE_INIT_FILE="$PWD/gradle/modern-artifactory.init.gradle"
export ARTIFACTORY_GRADLE_PLUGIN_URL='<approved plugin repository URL>'
export ARTIFACTORY_MAVEN_URL='<approved Maven repository URL>'
export INTERNAL_POSTGRES_IMAGE='<approved registry>/postgres@sha256:<digest>'
export INTERNAL_NODE_IMAGE='<approved registry>/node@sha256:<digest>'
export INTERNAL_JAVA_IMAGE='<approved registry>/temurin@sha256:<digest>'
export INTERNAL_JDK_IMAGE='<approved registry>/temurin-jdk@sha256:<digest>'
export INTERNAL_JRE_IMAGE='<approved registry>/temurin-jre@sha256:<digest>'
export INTERNAL_NGINX_IMAGE='<approved registry>/nginx@sha256:<digest>'
local-dev/generate-local-tls.sh
docker compose -p erp-local -f local-dev/docker-compose.yml up -d --build
local-dev/smoke-test.sh
```

## Public fallback for local testing

The sibling `../ofbiz-framework` checkout uses the Gradle Plugin Portal, Maven Central, and public base images. When approved internal coordinates are unavailable, the same approach can be selected explicitly for local testing:

```sh
export ARTIFACTORY_GRADLE_PLUGIN_URL='https://plugins.gradle.org/m2'
export ARTIFACTORY_MAVEN_URL='https://repo.maven.apache.org/maven2'
export INTERNAL_POSTGRES_IMAGE='postgres:13'
export INTERNAL_NODE_IMAGE='node:22-alpine'
export INTERNAL_JAVA_IMAGE='eclipse-temurin:17'
export INTERNAL_JDK_IMAGE='eclipse-temurin:17-jdk'
export INTERNAL_JRE_IMAGE='eclipse-temurin:17-jre'
export INTERNAL_NGINX_IMAGE='nginx:1.27-alpine'

local-dev/generate-local-tls.sh
docker compose -p erp-local -f local-dev/docker-compose.yml up -d --build
local-dev/smoke-test.sh
```

This fallback permits public Maven and container registry traffic. Do not use it for restricted CI or production builds.

Ports: `18080` is the preferred hybrid gateway; `18443` is direct legacy OFBiz fallback. The npm user config and corporate CA are BuildKit secrets only. They are neither copied into the build context nor persisted in image layers. The Gradle init script forces plugin and artifact resolution through the approved Artifactory repositories.
