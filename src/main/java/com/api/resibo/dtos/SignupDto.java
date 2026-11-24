package com.api.resibo.dtos;

import lombok.Data;

@Data
public class SignupDto {

    private String username;

    private String password;

    private String name;

    /**
     * Requested role: {@code ADMIN} or {@code EMPLOYEE}.
     *
     * <p>Honoured <b>only</b> when the caller is an authenticated ADMIN, or
     * when no users exist yet (first-run bootstrap). An anonymous signup to a
     * system that already has users always becomes EMPLOYEE, whatever is sent
     * here — otherwise anyone could grant themselves ADMIN.
     */
    private String role;
}
