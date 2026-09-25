FROM eclipse-temurin@sha256:e8d451f3b5aa6422c2b00bb913cb8d37a55a61934259109d945605c5651de9a6

RUN useradd --create-home --uid 10001 app \
    && mkdir --parents /var/lib/catalog \
    && chown app:app /var/lib/catalog
WORKDIR /app
COPY services/product-catalog-service/build/install/product-catalog-service/ ./
USER 10001
EXPOSE 8082
VOLUME ["/var/lib/catalog"]
ENTRYPOINT ["/app/bin/product-catalog-service"]
