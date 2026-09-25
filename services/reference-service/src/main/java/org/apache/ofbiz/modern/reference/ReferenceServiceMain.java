package org.apache.ofbiz.modern.reference;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;

public final class ReferenceServiceMain {
    private static final System.Logger LOGGER = System.getLogger(ReferenceServiceMain.class.getName());

    private ReferenceServiceMain() {
    }

    public static void main(String[] args) throws IOException {
        int port = configuredPort("REFERENCE_PORT", 8081);
        HttpServer server = ReferenceServiceApplication.createServer(port);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> server.stop(1)));
        server.start();
        LOGGER.log(System.Logger.Level.INFO, "reference-service-started port={0}", port);
    }

    private static int configuredPort(String environmentName, int fallback) {
        String value = System.getenv(environmentName);
        return value == null || value.isBlank() ? fallback : Integer.parseInt(value);
    }
}
