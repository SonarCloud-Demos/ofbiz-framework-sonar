ARG GRADLE_BUILD_IMAGE=required.invalid/internal/gradle:unset
ARG JAVA_RUNTIME_IMAGE=required.invalid/internal/java-runtime:unset

FROM ${GRADLE_BUILD_IMAGE} AS builder

WORKDIR /workspace

COPY gradle/modern-artifactory.init.gradle gradle/
COPY local-dev/healthcheck/org/apache/ofbiz/modern/healthcheck/HttpHealthProbe.java /tmp/HttpHealthProbe.java
COPY services/notification-service/ services/notification-service/

RUN javac -d /workspace/healthcheck /tmp/HttpHealthProbe.java

RUN --mount=type=secret,id=artifactory_token,target=/run/secrets/artifactory-token,required=true \
    ARTIFACTORY_TOKEN_FILE=/run/secrets/artifactory-token \
    gradle --no-daemon \
        --init-script /workspace/gradle/modern-artifactory.init.gradle \
        -p services/notification-service bootJar

FROM ${JAVA_RUNTIME_IMAGE}

WORKDIR /app
COPY --from=builder --chown=10001:10001 \
    /workspace/services/notification-service/build/libs/notification-service-*.jar \
    /app/notification-service.jar
COPY --from=builder --chown=10001:10001 /workspace/healthcheck/ /app/healthcheck/

USER 10001:10001
EXPOSE 8080
HEALTHCHECK --interval=5s --timeout=5s --start-period=15s --retries=12 \
    CMD ["java", "-cp", "/app/healthcheck", "org.apache.ofbiz.modern.healthcheck.HttpHealthProbe", "http://127.0.0.1:8080/api/notifications/health"]
ENTRYPOINT ["java", "-jar", "/app/notification-service.jar"]
