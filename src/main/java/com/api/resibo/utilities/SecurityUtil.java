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

    /** BCrypt comparison. Never use equals() on hashes — it leaks timing. */
    public boolean isPasswordMatched(String submittedPassword, String hashedPassword) {
        return passwordEncoder.matches(submittedPassword, hashedPassword);
    }

    /**
     * Username of the caller, or "manual" if nobody is authenticated.
     * Used for the created_by / updated_by columns.
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
