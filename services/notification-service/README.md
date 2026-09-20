# Modern Notification Service

This Java 17/Spring Boot service is the auxiliary Phase 1 proof of life. It intentionally exposes
only `GET /api/notifications/health`; notification delivery is outside the Accounting Invoices
strangler slice.

Build through the approved Artifactory configuration:

```sh
./gradlew --init-script "$PWD/gradle/modern-artifactory.init.gradle" \
    -p services/notification-service test
```
