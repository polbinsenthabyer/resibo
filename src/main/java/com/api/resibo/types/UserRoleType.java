package com.api.resibo.types;

/**
 * The roles the application understands.
 *
 * <p>These must match {@code role_name} values seeded in
 * sql/tables/user_role.sql. A role row whose name is not in this enum is
 * ignored by the application.
 *
 * <p>§1.3 of resibo-plan.md — who uses Resibo and what each must <em>not</em>
 * be able to do — is still open, so expect this list to grow.
 */
public enum UserRoleType {
    ADMIN,
    EMPLOYEE
}
