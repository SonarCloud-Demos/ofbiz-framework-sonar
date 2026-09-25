package org.apache.ofbiz.modern.bff;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.time.Duration;

public final class ExperienceBffMain {
    private static final System.Logger LOGGER = System.getLogger(ExperienceBffMain.class.getName());

    private ExperienceBffMain() {
    }

    public static void main(String[] args) throws IOException, GeneralSecurityException {
        int port = Integer.parseInt(environment("BFF_PORT", "8443"));
        URI referenceUri = URI.create(environment("REFERENCE_SERVICE_URL", "http://localhost:8081"));
        URI legacyUri = URI.create(environment("LEGACY_SERVICE_URL", "http://localhost:8080"));
        String username = environment("LOCAL_AUTH_USER", "modern-user");
        String password = requiredEnvironment("LOCAL_AUTH_PASSWORD");
        ExperienceBffApplication application = new ExperienceBffApplication(referenceUri, legacyUri,
                HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build(), username, password);
        HttpServer server = createServer(application, port);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> server.stop(1)));
        server.start();
        LOGGER.log(System.Logger.Level.INFO, "experience-bff-started port={0}", port);
    }

    private static HttpServer createServer(ExperienceBffApplication application, int port)
            throws IOException, GeneralSecurityException {
        String keyStorePath = environment("BFF_KEYSTORE_PATH", "");
        if (keyStorePath.isBlank()) {
            return application.createServer(port);
        }
        char[] password = environment("BFF_KEYSTORE_PASSWORD", "").toCharArray();
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (FileInputStream input = new FileInputStream(Path.of(keyStorePath).toFile())) {
            keyStore.load(input, password);
        }
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(
                KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, password);
        SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
        sslContext.init(keyManagerFactory.getKeyManagers(), null, null);
        HttpsServer server = HttpsServer.create(new java.net.InetSocketAddress(port), 0);
        server.setHttpsConfigurator(new HttpsConfigurator(sslContext));
        application.configure(server);
        return server;
    }

    private static String environment(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " must be configured");
        }
        return value;
    }
}
