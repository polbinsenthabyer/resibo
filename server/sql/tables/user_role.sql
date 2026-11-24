-- Resibo — user_role table
--
-- Roles live in a table rather than as an enum column so a role can be added
-- without a code change and a schema migration. Only ADMIN and EMPLOYEE exist
-- today; §1.3 of resibo-plan.md (who uses this, and what each must NOT be able
-- to do) is still open, and that answer is likely to add more.
--
-- Apply with the administrative account:
--   mariadb -h <host> -u <admin> -p resibo < sql/tables/user_role.sql
--
-- NOTE: the application maps role_name onto the UserRoleType enum. A row whose
-- role_name is not in that enum is ignored by the application, and removing a
-- row that users still reference will fail on the foreign key in user.sql.

CREATE TABLE IF NOT EXISTS user_role (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    role_name  VARCHAR(40) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by VARCHAR(40) NOT NULL DEFAULT 'manual',
    updated_by VARCHAR(40) NOT NULL DEFAULT 'manual',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                                    ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    -- Case-insensitive under utf8mb4_general_ci, so 'admin' and 'ADMIN' are
    -- the same role and cannot both be inserted.
    CONSTRAINT uq_user_role_name UNIQUE (role_name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

-- Seed the two roles the application knows about. INSERT IGNORE keeps this
-- file idempotent and will not disturb rows that already exist.
INSERT IGNORE INTO user_role (role_name, created_by, updated_by)
VALUES ('ADMIN', 'system', 'system'),
       ('EMPLOYEE', 'system', 'system');
