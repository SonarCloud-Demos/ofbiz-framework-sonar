package org.apache.ofbiz.modern.notification.config;

import org.apache.ofbiz.modern.notification.web.NotificationHealthResource;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JerseyConfig extends ResourceConfig {

    public JerseyConfig() {
        register(NotificationHealthResource.class);
    }
}
