package org.apache.ofbiz.modern.accounting.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.ofbiz.modern.accounting.domain.ServiceHealth;
import org.junit.jupiter.api.Test;

class ServiceHealthServiceTest {

    @Test
    void reportsTheModernAccountingServiceAsHealthy() {
        ServiceHealth health = new ServiceHealthService().getHealth();

        assertThat(health.status()).isEqualTo("UP");
        assertThat(health.service()).isEqualTo("modern-accounting-invoice-service");
    }
}
