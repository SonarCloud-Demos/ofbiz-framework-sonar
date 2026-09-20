package org.apache.ofbiz.modern.accounting.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

class IllegalArgumentExceptionMapperTest {

    @Test
    void mapsValidationFailuresToJsonBadRequests() {
        IllegalArgumentExceptionMapper mapper = new IllegalArgumentExceptionMapper();

        try (Response response = mapper.toResponse(new IllegalArgumentException("invoiceId is required"))) {
            assertThat(response.getStatus()).isEqualTo(400);
            assertThat(response.getMediaType().toString()).isEqualTo("application/json");
            assertThat(response.getEntity()).isEqualTo(
                    new IllegalArgumentExceptionMapper.ErrorResponse("INVALID_REQUEST", "invoiceId is required"));
        }
    }
}
