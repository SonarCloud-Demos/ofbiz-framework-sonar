package org.apache.ofbiz.modern.notification.web;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.springframework.stereotype.Component;

@Component
@Path("/api/notifications/health")
@Produces(MediaType.APPLICATION_JSON)
public class NotificationHealthResource {

    private static final HealthResponse HEALTH = new HealthResponse("UP", "modern-notification-service");

    @GET
    public HealthResponse getHealth() {
        return HEALTH;
    }

    public record HealthResponse(String status, String service) {
    }
}
