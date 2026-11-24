package com.api.resibo.repositories;

import com.api.resibo.entities.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Looks up a product by name for the uniqueness check.
     *
     * <p>Deliberately a plain equality match rather than {@code IgnoreCase}:
     * the column collates as {@code utf8mb4_general_ci}, so this is already
     * case-insensitive <em>and</em> can still use the unique index. An
     * {@code IgnoreCase} derivation would wrap the column in a function and
     * lose the index for no behavioural gain.
     *
     * <p>If the collation is ever changed to a case-sensitive one, this
     * method quietly becomes case-sensitive too — see sql/tables/product.sql.
     */
    Optional<Product> findByProductName(String productName);

    /**
     * Filtered, paginated search.
     *
     * <p>Every filter is optional: a null parameter disables that clause, so
     * one query serves the unfiltered listing and every combination of
     * filters, rather than branching across several repository methods.
     *
     * <p>Written as JPQL with bound parameters — no string concatenation — so
     * the filters cannot be used to inject SQL.
     */
    @Query("""
            SELECT p FROM Product p
            WHERE (:exactName IS NULL OR p.productName = :exactName)
              AND (:likeName  IS NULL OR LOWER(p.productName) LIKE LOWER(CONCAT('%', :likeName, '%')))
              AND (:minPrice  IS NULL OR p.productPrice >= :minPrice)
              AND (:maxPrice  IS NULL OR p.productPrice <= :maxPrice)
              AND (:minStock  IS NULL OR p.productRemainingStocks >= :minStock)
            """)
    Page<Product> search(String exactName,
                         String likeName,
                         BigDecimal minPrice,
                         BigDecimal maxPrice,
                         Integer minStock,
                         Pageable pageable);
}
