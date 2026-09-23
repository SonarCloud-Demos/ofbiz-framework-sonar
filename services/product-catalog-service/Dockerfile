ARG BUILD_IMAGE
ARG RUNTIME_IMAGE
FROM ${BUILD_IMAGE} AS build
ARG ARTIFACTORY_GRADLE_PLUGIN_URL
ARG ARTIFACTORY_MAVEN_URL
WORKDIR /workspace
COPY services/product-catalog-service/ ./
RUN --mount=type=secret,id=gradle_init,target=/run/secrets/gradle.init.gradle,required=true \
    --mount=type=secret,id=artifactory_user,target=/run/secrets/artifactory_user,required=true \
    --mount=type=secret,id=artifactory_token,target=/run/secrets/artifactory_token,required=true \
    --mount=type=secret,id=corporate_ca,target=/run/secrets/corporate-ca.pem,required=true \
    cp "${JAVA_HOME}/lib/security/cacerts" /tmp/modern-cacerts && \
    keytool -importcert -noprompt -storepass changeit -alias corporate-inspection \
      -file /run/secrets/corporate-ca.pem -keystore /tmp/modern-cacerts && \
    ARTIFACTORY_GRADLE_PLUGIN_URL="${ARTIFACTORY_GRADLE_PLUGIN_URL}" \
    ARTIFACTORY_MAVEN_URL="${ARTIFACTORY_MAVEN_URL}" \
    ARTIFACTORY_USER_FILE=/run/secrets/artifactory_user \
    ARTIFACTORY_TOKEN_FILE=/run/secrets/artifactory_token \
    GRADLE_OPTS="-Djavax.net.ssl.trustStore=/tmp/modern-cacerts -Djavax.net.ssl.trustStorePassword=changeit" \
    gradle --no-daemon --init-script /run/secrets/gradle.init.gradle clean bootJar && \
    rm -f /tmp/modern-cacerts

FROM ${RUNTIME_IMAGE}
WORKDIR /app
RUN addgroup --system catalog && adduser --system --ingroup catalog catalog
COPY --from=build --chown=catalog:catalog /workspace/build/libs/product-catalog-service-*.jar app.jar
USER catalog
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
