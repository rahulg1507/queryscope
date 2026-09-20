package com.queryscope.backend.service;

import com.queryscope.backend.model.HealthResponse;
import org.springframework.stereotype.Service;

@Service
public class HealthService {

    public HealthResponse getHealth() {
        return new HealthResponse("ok");
    }
}
