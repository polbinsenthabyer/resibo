package com.api.resibo.controllers;

import com.api.resibo.configs.SecurityConfig;
import com.api.resibo.configs.jwt.JwtAccessDeniedHandler;
import com.api.resibo.configs.jwt.JwtAuthenticationEntryPoint;
import com.api.resibo.configs.jwt.JwtFilter;
import com.api.resibo.configs.jwt.JwtUtil;
import com.api.resibo.utilities.MessageType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The real {@link SecurityConfig} is imported rather than disabled, so this
 * also proves the health endpoint is genuinely public — a rule change that
 * accidentally locked it down would fail here.
 *
 * <p>{@link JwtUtil} is mocked because it reads {@code jwt.secret} at startup,
 * which a slice test has no reason to supply.
 */
@WebMvcTest(HealthController.class)
@Import({SecurityConfig.class, JwtFilter.class,
        JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtUtil jwtUtil;

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

    @Test
    void healthCheckNeedsNoToken() throws Exception {
        // No Authorization header at all. If someone moves /api/health out of
        // the permitAll list, this fails with 401 instead of 200.
        mockMvc.perform(get("/api/health/check"))
                .andExpect(status().isOk());
    }
}
