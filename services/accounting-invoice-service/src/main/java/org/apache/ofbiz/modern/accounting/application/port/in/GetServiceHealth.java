package org.apache.ofbiz.modern.accounting.application.port.in;

public interface GetServiceHealth {

    HealthView getHealth();

    record HealthView(String status, String service) {
    }
}
