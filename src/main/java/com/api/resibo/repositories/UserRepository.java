package com.api.resibo.repositories;

import com.api.resibo.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Native SQL throughout — see {@link ProductRepository} for why. */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Case-insensitive via the column's {@code utf8mb4_general_ci} collation,
     * matching the unique constraint — so the name used to sign up is the name
     * that signs in, whatever the capitalisation.
     */
    @Query(value = "SELECT * FROM user WHERE username = :username", nativeQuery = true)
    Optional<User> findByUsername(@Param("username") String username);

    /**
     * Returns 1 or 0.
     *
     * <p>Declared as {@link Integer}, not {@code boolean}, on purpose: MariaDB
     * answers {@code EXISTS} with an integer, and binding that straight onto a
     * boolean return throws
     * {@code ClassCastException: Integer cannot be cast to Boolean} at
     * runtime — a failure JPQL would never have produced, and one that
     * surfaces as a confusing 401 rather than an obvious error. The
     * conversion is done explicitly below.
     */
    @Query(value = "SELECT EXISTS(SELECT 1 FROM user WHERE username = :username)", nativeQuery = true)
    Integer countByUsername(@Param("username") String username);

    /** Used only by the first-run bootstrap. Same Integer caveat as above. */
    @Query(value = "SELECT EXISTS(SELECT 1 FROM user LIMIT 1)", nativeQuery = true)
    Integer countAnyUser();

    default boolean existsByUsername(String username) {
        Integer found = countByUsername(username);
        return found != null && found != 0;
    }

    default boolean anyUserExists() {
        Integer found = countAnyUser();
        return found != null && found != 0;
    }
}
