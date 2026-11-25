package com.api.resibo.dtos;

import lombok.Data;

/**
 * Create-or-update payload for an admin-managed account.
 *
 * <p>{@code id} null means create, set means update — same convention as
 * {@link ProductDto}. Unlike Product, two fields behave differently on
 * update than on create: a blank or null {@code password} on update means
 * "leave unchanged", not "clear the password", and a blank or null
 * {@code role} on update means "leave unchanged", not "reset to the
 * create-time default". Activation state is never set here — see
 * {@code AdminUserController#activateUser}/{@code deactivateUser}.
 */
@Data
public class UserDto {

    private Long id;

    private String username;

    /** Required (>=8 chars) on create. Optional on update — blank/null means unchanged. */
    private String password;

    private String name;

    /** "ADMIN" or "EMPLOYEE". Create: blank means EMPLOYEE. Update: blank means unchanged. */
    private String role;
}
