FROM eclipse-temurin@sha256:e8d451f3b5aa6422c2b00bb913cb8d37a55a61934259109d945605c5651de9a6

RUN useradd --create-home --uid 10001 app
WORKDIR /app
COPY services/reference-service/build/install/reference-service/ ./
USER 10001
EXPOSE 8081
ENTRYPOINT ["/app/bin/reference-service"]
