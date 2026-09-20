package org.apache.ofbiz.modern.accounting.application;

import org.apache.ofbiz.modern.accounting.domain.ServiceHealth;

public class ServiceHealthService {

    private static final String SERVICE_NAME = "modern-accounting-invoice-service";

    public ServiceHealth getHealth() {
        return new ServiceHealth("UP", SERVICE_NAME);
    }
}
