package com.api.resibo.dtos;

import lombok.Data;

/** Request body for the admin user listing: filters, sorting and pagination. */
@Data
public class UserListDto {

    /** Matched against username. Ignored when null or blank. */
    private String username;

    /** True for an exact username match; otherwise a contains-match is used. */
    private Boolean isExactMatch;

    /** "ADMIN" or "EMPLOYEE". Ignored when null or blank. */
    private String role;

    /** True/false to filter by active status. Ignored when null. */
    private Boolean isActive;

    private String sortBy = "id";

    private String sortDirection = "ASC";

    private Integer page = 0;

    private Integer size = 10;
}
