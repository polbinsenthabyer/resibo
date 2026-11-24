package com.api.resibo.repositories;

import com.api.resibo.entities.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Native SQL throughout, on the owner's instruction.
 *
 * <p>Two consequences worth knowing. Parameters and columns are the
 * <em>database</em> names (`product_name`), not entity field names
 * (`productName`) — which also means any {@code Sort} passed in a
 * {@link Pageable} must carry column names, so the service translates them.
 * And a paginated native query needs its own {@code countQuery}; without one
 * Spring Data cannot work out the total.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Looks up a product by name for the uniqueness check.
     *
     * <p>Plain equality rather than {@code LOWER(...)}: the column collates as
     * {@code utf8mb4_general_ci}, so this is already case-insensitive
     * <em>and</em> can still use the unique index. Wrapping the column in a
     * function would lose the index for no behavioural gain.
     */
    @Query(value = "SELECT * FROM product WHERE product_name = :productName", nativeQuery = true)
    Optional<Product> findByProductName(@Param("productName") String productName);

    /**
     * Filtered, paginated search.
     *
     * <p>Every filter is optional: a null parameter disables its clause, so
     * one query serves the unfiltered listing and every combination of
     * filters rather than branching across several methods.
     *
     * <p>All values are bound parameters — no string concatenation — so the
     * filters cannot be used to inject SQL. {@code LIKE} needs no
     * {@code LOWER()} for the same collation reason as above.
     */
    @Query(value = """
            SELECT * FROM product
            WHERE (:exactName IS NULL OR product_name = :exactName)
              AND (:likeName  IS NULL OR product_name LIKE CONCAT('%', :likeName, '%'))
              AND (:minPrice  IS NULL OR product_price >= :minPrice)
              AND (:maxPrice  IS NULL OR product_price <= :maxPrice)
              AND (:minStock  IS NULL OR product_remaining_stocks >= :minStock)
            """,
            countQuery = """
            SELECT COUNT(*) FROM product
            WHERE (:exactName IS NULL OR product_name = :exactName)
              AND (:likeName  IS NULL OR product_name LIKE CONCAT('%', :likeName, '%'))
              AND (:minPrice  IS NULL OR product_price >= :minPrice)
              AND (:maxPrice  IS NULL OR product_price <= :maxPrice)
              AND (:minStock  IS NULL OR product_remaining_stocks >= :minStock)
            """,
            nativeQuery = true)
    Page<Product> search(@Param("exactName") String exactName,
                         @Param("likeName") String likeName,
                         @Param("minPrice") BigDecimal minPrice,
                         @Param("maxPrice") BigDecimal maxPrice,
                         @Param("minStock") Integer minStock,
                         Pageable pageable);
}
