package com.api.resibo.repositories;

import com.api.resibo.entities.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Native SQL throughout — see {@link ProductRepository} for why. */
@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    /** Case-insensitive under the column's collation, like the unique index. */
    @Query(value = "SELECT * FROM user_role WHERE role_name = :roleName", nativeQuery = true)
    Optional<UserRole> findByRoleName(@Param("roleName") String roleName);

    @Query(value = "SELECT * FROM user_role WHERE id = :id", nativeQuery = true)
    Optional<UserRole> findRoleById(@Param("id") Long id);
}
