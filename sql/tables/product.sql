-- Resibo — product table
--
-- Hibernate runs with ddl-auto: none, so the application will never create or
-- alter this table. Apply this file by hand (or through a migration tool once
-- one is adopted) using the administrative database account — the application
-- account is not meant to run schema changes even though it technically could
-- within its own schema.
--
--   mariadb -h <host> -u <admin> -p resibo < sql/tables/product.sql
--
-- Charset utf8mb4 with COLLATE utf8mb4_general_ci, the project-wide standard
-- for every table -- see sql/README.md. Note this is NOT the MariaDB 11.8
-- server default (utf8mb4_uca1400_ai_ci), so it is stated explicitly on every
-- table rather than inherited.

CREATE TABLE IF NOT EXISTS product (
    id                       BIGINT        NOT NULL AUTO_INCREMENT,
    product_name             VARCHAR(100)  NOT NULL,
    product_description      VARCHAR(250)      NULL,
    product_price            DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    product_remaining_stocks INT           NOT NULL DEFAULT 0,
    created_at               DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by               VARCHAR(40)   NOT NULL DEFAULT 'manual',
    updated_by               VARCHAR(40)   NOT NULL DEFAULT 'manual',
    updated_at               DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                                                    ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    -- Not in the original specification. Added because the list endpoint
    -- filters and sorts on product_name, and without it every page scans the
    -- whole table. Drop it if you would rather keep the schema minimal.
    KEY idx_product_name (product_name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;
