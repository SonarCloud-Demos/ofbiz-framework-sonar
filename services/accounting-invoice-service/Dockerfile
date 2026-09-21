# syntax=docker/dockerfile:1
ARG MODERN_JDK_IMAGE
ARG MODERN_JRE_IMAGE
FROM ${MODERN_JDK_IMAGE} AS build
ARG ARTIFACTORY_GRADLE_PLUGIN_URL
ARG ARTIFACTORY_MAVEN_URL
ENV ARTIFACTORY_GRADLE_PLUGIN_URL=${ARTIFACTORY_GRADLE_PLUGIN_URL} \
    ARTIFACTORY_MAVEN_URL=${ARTIFACTORY_MAVEN_URL}
WORKDIR /workspace
COPY gradlew settings.gradle build.gradle ./
COPY gradle/ gradle/
COPY services/accounting-invoice-service/ services/accounting-invoice-service/
RUN --mount=type=secret,id=gradle_init,target=/root/.gradle/init.gradle,required=false \
    ./gradlew -p services/accounting-invoice-service bootJar --no-daemon

FROM ${MODERN_JRE_IMAGE}
RUN useradd --system --uid 10001 modern
USER modern
WORKDIR /app
COPY --from=build /workspace/services/accounting-invoice-service/build/libs/accounting-invoice-service-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
