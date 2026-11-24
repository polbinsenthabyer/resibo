package com.api.resibo.configs.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Issues and reads JWTs.
 *
 * <p>Uses the jjwt 0.13 API, which differs from the 0.11 API gainsoclock is
 * on — do not copy token code between the two projects without adjusting it.
 */
@Component
public class JwtUtil {

    public static final String CLAIM_ROLE = "role";

    @Value("${jwt.secret}")
    private String jwtSecret;

    /** Token lifetime in milliseconds. */
    @Value("${jwt.expiration}")
    private long jwtExpirationMillis;

    private SecretKey signingKey;

    /**
     * Built once at startup rather than per call. That also means a secret too
     * short for HS256 fails the application at boot instead of at the first
     * login attempt. HS256 needs at least 256 bits — 32 ASCII characters —
     * and jjwt refuses anything shorter.
     */
    @PostConstruct
    void init() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "jwt.secret must be at least 32 characters for HS256; got " + keyBytes.length);
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String username, String role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(username)
                .claim(CLAIM_ROLE, role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + jwtExpirationMillis))
                .signWith(signingKey)
                .compact();
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** True only for a token this server signed that has not expired. */
    public boolean isTokenValid(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
