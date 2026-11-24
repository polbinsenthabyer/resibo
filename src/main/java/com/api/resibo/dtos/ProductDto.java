package com.api.resibo.dtos;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Create-or-update payload for a product.
 *
 * <p>There is no separate update endpoint: {@code id} decides which happens.
 * Null means create; a populated id means update that product. Audit columns
 * are set by the service, never accepted from the caller.
 */
@Data
public class ProductDto {

    /** Null to create a new product; set to update an existing one. */
    private Long id;

    private String productName;

    private String productDescription;

    /** Defaults to 0.00 when omitted, matching the column default. */
    private BigDecimal productPrice;

    /** Defaults to 0 when omitted, matching the column default. */
    private Integer productRemainingStocks;
}
