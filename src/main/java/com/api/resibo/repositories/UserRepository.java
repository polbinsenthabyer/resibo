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

    /** Returns 1 when the username is taken, 0 otherwise. */
    @Query(value = "SELECT EXISTS(SELECT 1 FROM user WHERE username = :username)", nativeQuery = true)
    boolean existsByUsername(@Param("username") String username);

    /**
     * Used only by the first-run bootstrap, to decide whether a signup may
     * become ADMIN. {@code LIMIT 1} because the question is "is it empty?",
     * not "how many".
     */
    @Query(value = "SELECT EXISTS(SELECT 1 FROM user LIMIT 1)", nativeQuery = true)
    boolean anyUserExists();
}
