package com.api.resibo.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * What a successful login returns.
 *
 * <p>Carries the role so a frontend can decide what to render without
 * decoding the token. The token remains the only thing the server trusts —
 * a client editing this field changes nothing about what it may do.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDto {
    private String username;
    private String role;
    private String token;
    /** Token lifetime in seconds, so a client can refresh before it expires. */
    private long expiresInSeconds;
}
