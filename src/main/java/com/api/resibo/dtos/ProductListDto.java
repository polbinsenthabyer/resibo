package com.api.resibo.dtos;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Request body for the product listing: filters, sorting and pagination.
 *
 * <p>Every field is optional. Defaults give a sane first page without the
 * caller having to send anything, but an empty body still has to be a valid
 * JSON object ({@code {}}).
 */
@Data
public class ProductListDto {

    // --- filters ----------------------------------------------------------

    /** Matched against product_name. Ignored when null or blank. */
    private String productName;

    /** True for an exact name match; otherwise a contains-match is used. */
    private Boolean isExactMatch;

    /** Inclusive lower bound on price. Ignored when null. */
    private BigDecimal minPrice;

    /** Inclusive upper bound on price. Ignored when null. */
    private BigDecimal maxPrice;

    /** True to return only products with remaining stock above zero. */
    private Boolean inStockOnly;

    // --- sorting ----------------------------------------------------------

    /**
     * Property to sort by. Restricted to a known set by the service — an
     * unrecognised value falls back to {@code id} rather than failing, so a
     * typo cannot take the endpoint down.
     */
    private String sortBy = "id";

    /** ASC or DESC, case-insensitive. Anything else is treated as ASC. */
    private String sortDirection = "ASC";

    // --- pagination -------------------------------------------------------

    /** Zero-based page index. Negative values are clamped to 0. */
    private Integer page = 0;

    /** Rows per page. Clamped to 1..200 to bound the response size. */
    private Integer size = 10;
}
