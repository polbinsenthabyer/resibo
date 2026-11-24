package com.api.resibo.controllers;

import com.api.resibo.dtos.ResponseDto;
import com.api.resibo.utilities.MessageType;
import com.api.resibo.utilities.ResponseUtility;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Liveness check for the API itself. Distinct from {@code /actuator/health},
 * which reports infrastructure state; this one proves the application's own
 * request path and response envelope are working.
 */
@RestController
@RequestMapping("api/health")
public class HealthController {

    @GetMapping("/check")
    public ResponseDto healthCheck() {
        return ResponseUtility.getSuccessResponse(MessageType.API_HEALTH_CHECK);
    }
}
