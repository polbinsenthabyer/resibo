package com.api.resibo.utilities;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityUtil {

    private final PasswordEncoder passwordEncoder;

    public String hashPassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    /**
     * Constant-time comparison via BCrypt. Never compare hashes with
     * {@code equals} — that leaks timing information.
     */
    public boolean isPasswordMatched(String submittedPassword, String hashedPassword) {
        return passwordEncoder.matches(submittedPassword, hashedPassword);
    }

    /**
     * Username of the caller, or {@code "manual"} when there is no
     * authenticated principal.
     *
     * <p>This is what finally makes the audit columns meaningful: before
     * authentication existed every row said "manual", which recorded nothing.
     */
    public String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
            return "manual";
        }
        String name = auth.getName();
        return name == null || name.isBlank() ? "manual" : name;
    }
}
