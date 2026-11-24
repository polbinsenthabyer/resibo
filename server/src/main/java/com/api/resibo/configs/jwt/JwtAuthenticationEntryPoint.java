package com.api.resibo.configs.jwt;

import com.api.resibo.utilities.MessageType;
import com.api.resibo.utilities.ResponseUtility;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Handles requests to protected endpoints that have no valid token.
 *
 * Returns 401 with the normal ResponseDto envelope, so clients parse every
 * failure the same way. Note this is a real 401, unlike business errors which
 * return 200 with an error code — the request never reached the application.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(
                response.getWriter(),
                ResponseUtility.getErrorResponse("E0401", MessageType.ERROR_UNAUTHENTICATED));
    }
}
