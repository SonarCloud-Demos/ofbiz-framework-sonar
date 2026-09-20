package org.apache.ofbiz.modern.accounting.adapter.in.web;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.ofbiz.modern.accounting.application.port.in.GetServiceHealth;
import org.springframework.stereotype.Component;

@Component
@Path("/api/accounting/invoices/health")
@Produces(MediaType.APPLICATION_JSON)
public class HealthResource {

    private final GetServiceHealth getServiceHealth;

    public HealthResource(GetServiceHealth getServiceHealth) {
        this.getServiceHealth = getServiceHealth;
    }

    @GET
    public GetServiceHealth.HealthView getHealth() {
        return getServiceHealth.getHealth();
    }
}
