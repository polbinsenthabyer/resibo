package com.api.resibo.controllers;

import com.api.resibo.dtos.LoginDto;
import com.api.resibo.dtos.ResponseDto;
import com.api.resibo.dtos.SignupDto;
import com.api.resibo.services.auth.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication. The only endpoints under {@code /api} that do not require a
 * token — see {@code SecurityConfig}.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("api/auth")
public class AuthController {

    private final AuthService authService;

    /**
     * Registers an account.
     *
     * <p>⚠️ Public. The first account created on an empty system becomes
     * ADMIN; after that an anonymous signup is always EMPLOYEE regardless of
     * the requested role, and only an authenticated ADMIN can create another
     * ADMIN.
     */
    @PostMapping("/signup")
    public ResponseDto signup(@RequestBody SignupDto dto) {
        return authService.signup(dto);
    }

    /** Exchanges username and password for a bearer token. */
    @PostMapping("/login")
    public ResponseDto login(@RequestBody LoginDto dto) {
        return authService.login(dto);
    }
}
