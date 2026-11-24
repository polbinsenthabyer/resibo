package com.api.resibo.configs.jwt;

import com.api.resibo.utilities.MessageType;
import com.api.resibo.utilities.ResponseUtility;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Returned when the caller is authenticated but lacks the required role — an
 * EMPLOYEE token reaching an {@code /api/admin/**} endpoint, for instance.
 *
 * <p><b>403</b>, deliberately distinct from the 401 for "not signed in": a
 * client should not respond to this by trying to log in again.
 */
@Component
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(
                response.getWriter(),
                ResponseUtility.getErrorResponse("E0403", MessageType.ERROR_NO_PERMISSION));
    }
}
