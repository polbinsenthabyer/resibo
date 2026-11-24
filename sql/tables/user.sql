-- Resibo — user table
--
-- Depends on user_role: apply sql/tables/user_role.sql FIRST, or the foreign
-- key will fail.
--
--   mariadb -h <host> -u <admin> -p resibo < sql/tables/user_role.sql
--   mariadb -h <host> -u <admin> -p resibo < sql/tables/user.sql
--
-- `password` holds a BCrypt hash, never a plaintext password. BCrypt output is
-- 60 characters; the column is 100 to leave room for a future algorithm change
-- without a migration.

CREATE TABLE IF NOT EXISTS user (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    username      VARCHAR(100) NOT NULL,
    password      VARCHAR(100) NOT NULL,
    name          VARCHAR(100)     NULL,
    user_role_id  BIGINT       NOT NULL,
    -- Lets an account be disabled without deleting it, which matters once the
    -- user is referenced by sales records.
    is_active     TINYINT(1)   NOT NULL DEFAULT 1,
    last_login_at DATETIME(3)      NULL,
    created_at    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by    VARCHAR(40)  NOT NULL DEFAULT 'manual',
    updated_by    VARCHAR(40)  NOT NULL DEFAULT 'manual',
    updated_at    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                                       ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    -- Case-insensitive under utf8mb4_general_ci: 'Pol' and 'pol' are the same
    -- account and cannot both be registered. Login matches the same way.
    CONSTRAINT uq_user_username UNIQUE (username),
    CONSTRAINT fk_user_user_role
        FOREIGN KEY (user_role_id) REFERENCES user_role (id)
        -- RESTRICT, not CASCADE: deleting a role must never silently delete
        -- the people who hold it.
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;
