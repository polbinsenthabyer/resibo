package com.api.resibo.utilities;

import com.api.resibo.entities.User;
import com.api.resibo.entities.UserRole;
import com.api.resibo.repositories.UserRepository;
import com.api.resibo.repositories.UserRoleRepository;
import com.api.resibo.types.UserRoleType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Arrays;
import java.util.Optional;

/**
 * User and role lookups, so services do not each wire up two repositories.
 *
 * Based on gainsoclock's UserUtility, with three changes:
 *  - returns entities, not ResponseDto; building the response is the
 *    service's job
 *  - returns null for a missing row instead of throwing NPE
 *  - the exists checks actually check (gainsoclock's isUserExistById tests an
 *    object that is never null, so it always returns true)
 */
@Component
@RequiredArgsConstructor
public class UserUtility {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    // --- lookups ----------------------------------------------------------

    /** Case-insensitive, via the column collation. Null when not found. */
    public User findByUsername(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        return userRepository.findByUsername(username.trim()).orElse(null);
    }

    public User findById(Long userId) {
        return userId == null ? null : userRepository.findById(userId).orElse(null);
    }

    /** The authenticated caller, or null when the request is anonymous. */
    public User findUserByPrincipal(Principal principal) {
        return principal == null ? null : findByUsername(principal.getName());
    }

    public boolean isUserExistByUsername(String username) {
        return username != null
                && !username.isBlank()
                && userRepository.existsByUsername(username.trim());
    }

    public boolean isUserExistById(Long userId) {
        return userId != null && userRepository.existsById(userId);
    }

    /** True when the system has no accounts — the first-run bootstrap case. */
    public boolean isFirstUser() {
        return !userRepository.anyUserExists();
    }

    // --- roles ------------------------------------------------------------

    public UserRole findRoleByRoleId(Long roleId) {
        return roleId == null ? null : userRoleRepository.findRoleById(roleId).orElse(null);
    }

    /**
     * The role row for a {@link UserRoleType}, or null when the row is absent
     * — which means sql/tables/user_role.sql was never applied.
     */
    public UserRole findRoleByRoleType(UserRoleType roleType) {
        return roleType == null ? null : userRoleRepository.findByRoleName(roleType.name()).orElse(null);
    }

    public Long findRoleIdByRoleType(UserRoleType roleType) {
        UserRole role = findRoleByRoleType(roleType);
        return role == null ? null : role.getId();
    }

    /**
     * The user's role as an enum, or null when the stored role name is not one
     * the application knows about — possible if a row was added to
     * {@code user_role} by hand.
     */
    public UserRoleType roleTypeOf(User user) {
        if (user == null) {
            return null;
        }
        UserRole role = findRoleByRoleId(user.getUserRoleId());
        if (role == null || role.getRoleName() == null) {
            return null;
        }
        return Arrays.stream(UserRoleType.values())
                .filter(t -> t.name().equalsIgnoreCase(role.getRoleName()))
                .findFirst()
                .orElse(null);
    }

    public boolean hasAnyRole(User user, UserRoleType... roles) {
        UserRoleType actual = roleTypeOf(user);
        return actual != null && Arrays.asList(roles).contains(actual);
    }

    public boolean isAdmin(User user) {
        return hasAnyRole(user, UserRoleType.ADMIN);
    }

    public boolean isActive(User user) {
        return user != null && Boolean.TRUE.equals(user.getIsActive());
    }

    /**
     * Role name as stored, for putting in a token.
     *
     * <p>Returns {@link Optional} because a user whose role row is missing has
     * no usable role and the caller must decide what that means, rather than
     * being handed a null to sign into a token.
     */
    public Optional<String> roleNameOf(User user) {
        if (user == null) {
            return Optional.empty();
        }
        UserRole role = findRoleByRoleId(user.getUserRoleId());
        return Optional.ofNullable(role).map(UserRole::getRoleName);
    }
}
