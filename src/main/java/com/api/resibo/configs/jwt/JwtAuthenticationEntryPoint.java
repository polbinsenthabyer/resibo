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
 * Returned when a request reaches a protected endpoint without a usable token.
 *
 * <p>Answers with the project's own {@code ResponseDto} envelope rather than
 * Spring's default error body, so a client parses every failure the same way.
 *
 * <p>The HTTP status is a real <b>401</b>, unlike the business errors which
 * return 200 with an error code — this is a transport-level rejection that
 * never reached application logic.
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
