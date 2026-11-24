package com.api.resibo.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * A sellable product.
 *
 * <p>The table is defined in {@code sql/tables/product.sql} and owned
 * outside the application — Hibernate runs with {@code ddl-auto: none} and will
 * not create or alter it. If a field here stops matching that file, the file is
 * the authority.
 */
@Entity
@Data
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "product_description")
    private String productDescription;

    /** BigDecimal, never double — money must not be subject to binary rounding. */
    @Column(name = "product_price")
    private BigDecimal productPrice;

    @Column(name = "product_remaining_stocks")
    private Integer productRemainingStocks;

    @Column(name = "created_by")
    private String createdBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS")
    @Column(name = "created_at")
    private Date createdAt;

    @Column(name = "updated_by")
    private String updatedBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS")
    @Column(name = "updated_at")
    private Date updatedAt;
}
