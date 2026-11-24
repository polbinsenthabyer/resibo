package com.api.resibo.controllers;

import com.api.resibo.utilities.MessageType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HealthController.class)
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthCheckReturnsSuccessEnvelope() throws Exception {
        mockMvc.perform(get("/api/health/check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseType").value("SUCCESS"))
                .andExpect(jsonPath("$.code").value("S0001"))
                .andExpect(jsonPath("$.message").value(MessageType.API_HEALTH_CHECK))
                .andExpect(jsonPath("$.responseTime").exists())
                // simple name, not the fully-qualified com.api.resibo.controllers.HealthController
                .andExpect(jsonPath("$.className").value("HealthController"))
                .andExpect(jsonPath("$.methodName").value("healthCheck"));
    }
}
