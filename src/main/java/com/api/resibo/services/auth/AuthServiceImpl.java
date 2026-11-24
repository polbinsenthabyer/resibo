package com.api.resibo.services.auth;

import com.api.resibo.configs.jwt.JwtUtil;
import com.api.resibo.dtos.LoginDto;
import com.api.resibo.dtos.LoginResponseDto;
import com.api.resibo.dtos.ResponseDto;
import com.api.resibo.dtos.SignupDto;
import com.api.resibo.entities.User;
import com.api.resibo.entities.UserRole;
import com.api.resibo.repositories.UserRepository;
import com.api.resibo.types.UserRoleType;
import com.api.resibo.utilities.MessageType;
import com.api.resibo.utilities.ResponseUtility;
import com.api.resibo.utilities.SecurityUtil;
import com.api.resibo.utilities.UserUtility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserUtility userUtility;
    private final SecurityUtil securityUtil;
    private final JwtUtil jwtUtil;

    private static final int MIN_PASSWORD_LENGTH = 8;

    @Override
    @Transactional
    public ResponseDto signup(SignupDto dto) {
        if (dto == null) {
            return ResponseUtility.getErrorResponse("E0001", MessageType.ERROR_DTO_IS_NULL_OR_INCOMPLETE);
        }

        String username = dto.getUsername() == null ? null : dto.getUsername().trim();
        if (username == null || username.isEmpty()) {
            return ResponseUtility.getErrorResponse("E0101", MessageType.ERROR_USERNAME_REQUIRED);
        }
        if (username.length() > 100) {
            return ResponseUtility.getErrorResponse("E0102", MessageType.ERROR_USERNAME_TOO_LONG);
        }
        if (dto.getPassword() == null || dto.getPassword().length() < MIN_PASSWORD_LENGTH) {
            return ResponseUtility.getErrorResponse("E0103", MessageType.ERROR_PASSWORD_TOO_SHORT);
        }
        if (userUtility.isUserExistByUsername(username)) {
            return ResponseUtility.getErrorResponse("E0104", MessageType.ERROR_USERNAME_ALREADY_EXISTS);
        }

        UserRoleType requested = parseRole(dto.getRole());
        if (dto.getRole() != null && !dto.getRole().isBlank() && requested == null) {
            return ResponseUtility.getErrorResponse("E0105", MessageType.ERROR_INVALID_ROLE);
        }
        UserRoleType granted = resolveGrantedRole(requested);

        UserRole role = userUtility.findRoleByRoleType(granted);
        if (role == null) {
            // The role rows are seeded by sql/tables/user_role.sql. If they are
            // missing the schema was not fully applied — that is an operator
            // problem, not a caller problem, so it is logged loudly.
            log.error("Role {} is missing from user_role. Apply sql/tables/user_role.sql.", granted);
            return ResponseUtility.getErrorResponse("E9999", MessageType.ERROR_SOMETHING_WENT_WRONG);
        }

        try {
            Date now = new Date();
            String actor = securityUtil.currentUsername();

            User user = new User();
            user.setUsername(username);
            user.setPassword(securityUtil.hashPassword(dto.getPassword()));
            user.setName(dto.getName());
            user.setUserRoleId(role.getId());
            user.setIsActive(Boolean.TRUE);
            user.setCreatedBy(actor);
            user.setCreatedAt(now);
            user.setUpdatedBy(actor);
            user.setUpdatedAt(now);
            userRepository.save(user);

            // The created account is NOT returned — not even without the hash.
            // Signup confirms, it does not disclose.
            return ResponseUtility.getSuccessResponse(MessageType.SUCCESSFULLY_SIGNED_UP);
        } catch (DataIntegrityViolationException e) {
            // Lost the race against a concurrent signup for the same username.
            log.warn("signup hit the unique constraint for username={}", username);
            return ResponseUtility.getErrorResponse("E0104", MessageType.ERROR_USERNAME_ALREADY_EXISTS);
        } catch (Exception e) {
            log.error("signup failed for username={}", username, e);
            return ResponseUtility.getErrorResponse("E9999", MessageType.ERROR_SOMETHING_WENT_WRONG);
        }
    }

    @Override
    @Transactional
    public ResponseDto login(LoginDto dto) {
        if (dto == null) {
            return ResponseUtility.getErrorResponse("E0001", MessageType.ERROR_DTO_IS_NULL_OR_INCOMPLETE);
        }

        String username = dto.getUsername() == null ? null : dto.getUsername().trim();
        if (username == null || username.isEmpty() || dto.getPassword() == null) {
            return ResponseUtility.getErrorResponse("E0106", MessageType.ERROR_INVALID_CREDENTIALS);
        }

        User user = userUtility.findByUsername(username);

        // One code and one message whether the account is missing, disabled or
        // the password is wrong. Distinguishing them tells an attacker which
        // usernames are real.
        if (user == null
                || !userUtility.isActive(user)
                || !securityUtil.isPasswordMatched(dto.getPassword(), user.getPassword())) {
            log.info("failed login attempt for username={}", username);
            return ResponseUtility.getErrorResponse("E0106", MessageType.ERROR_INVALID_CREDENTIALS);
        }

        UserRole role = userUtility.findRoleByRoleId(user.getUserRoleId());
        if (role == null) {
            log.error("user {} references missing role id {}", username, user.getUserRoleId());
            return ResponseUtility.getErrorResponse("E9999", MessageType.ERROR_SOMETHING_WENT_WRONG);
        }

        user.setLastLoginAt(new Date());
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getUsername(), role.getRoleName());
        LoginResponseDto payload = new LoginResponseDto(
                user.getUsername(), role.getRoleName(), token);

        return ResponseUtility.getSuccessResponse(MessageType.SUCCESSFULLY_LOGGED_IN, payload);
    }

    private UserRoleType parseRole(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UserRoleType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Works out which role a signup actually gets.
     *
     * Anonymous callers cannot pick their own role. The exception is the very
     * first account, which becomes ADMIN — otherwise there is no way to get an
     * admin without editing the database by hand.
     *
     * Warning: while signup is public and the table is empty, whoever signs up
     * first becomes admin. Create the admin account right after deploying.
     */
    private UserRoleType resolveGrantedRole(UserRoleType requested) {
        if (userUtility.isFirstUser()) {
            return requested == null ? UserRoleType.ADMIN : requested;
        }
        return isCallerAdmin() && requested != null ? requested : UserRoleType.EMPLOYEE;
    }

    private boolean isCallerAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null
                && auth.isAuthenticated()
                && auth.getAuthorities().stream()
                        .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}
