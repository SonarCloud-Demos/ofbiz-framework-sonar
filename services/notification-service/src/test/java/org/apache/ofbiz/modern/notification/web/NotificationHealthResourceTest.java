package org.apache.ofbiz.modern.notification.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NotificationHealthResourceTest {

    @Test
    void reportsTheServiceAsHealthy() {
        NotificationHealthResource.HealthResponse response = new NotificationHealthResource().getHealth();

        assertThat(response.status()).isEqualTo("UP");
        assertThat(response.service()).isEqualTo("modern-notification-service");
    }
}
