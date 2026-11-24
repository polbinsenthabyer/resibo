package com.api.resibo.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * What a successful login returns.
 *
 * Includes the role so a frontend can decide what to show without decoding
 * the token. The token is still the only thing the server trusts, so editing
 * this field client-side changes nothing about what the caller may do.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDto {
    private String username;
    private String role;
    private String token;
}
