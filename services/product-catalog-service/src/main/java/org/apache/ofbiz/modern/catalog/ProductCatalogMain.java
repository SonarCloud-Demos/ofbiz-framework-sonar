package org.apache.ofbiz.modern.catalog;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;

public final class ProductCatalogMain {
    private static final System.Logger LOGGER = System.getLogger(ProductCatalogMain.class.getName());

    private ProductCatalogMain() {
    }

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(System.getenv().getOrDefault("CATALOG_PORT", "8082"));
        HttpServer server = ProductCatalogApplication.load().createServer(port);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> server.stop(1)));
        server.start();
        LOGGER.log(System.Logger.Level.INFO, "product-catalog-service-started port={0}", port);
    }
}
