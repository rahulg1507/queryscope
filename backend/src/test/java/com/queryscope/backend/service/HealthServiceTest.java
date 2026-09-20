package com.queryscope.backend.service;

import com.queryscope.backend.model.HealthResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HealthServiceTest {

    private final HealthService healthService = new HealthService();

    @Test
    void returnsOkStatus() {
        HealthResponse response = healthService.getHealth();

        assertThat(response.status()).isEqualTo("ok");
    }
}
