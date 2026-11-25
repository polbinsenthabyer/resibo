package com.api.resibo.services.user;

import com.api.resibo.dtos.ResponseDto;
import com.api.resibo.dtos.UserDto;
import com.api.resibo.dtos.UserListDto;
import com.api.resibo.entities.User;
import com.api.resibo.repositories.UserRepository;
import com.api.resibo.types.UserRoleType;
import com.api.resibo.utilities.MessageType;
import com.api.resibo.utilities.ResponseUtility;
import com.api.resibo.utilities.SecurityUtil;
import com.api.resibo.utilities.UserUtility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserUtility userUtility;
    private final SecurityUtil securityUtil;

    private static final int MIN_PASSWORD_LENGTH = 8;

    @Override
    @Transactional
    public ResponseDto setUser(UserDto dto) {
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

        boolean isCreate = dto.getId() == null;

        String rawPassword = dto.getPassword();
        boolean passwordProvided = rawPassword != null && !rawPassword.isBlank();
        if (isCreate) {
            if (!passwordProvided || rawPassword.length() < MIN_PASSWORD_LENGTH) {
                return ResponseUtility.getErrorResponse("E0103", MessageType.ERROR_PASSWORD_TOO_SHORT);
            }
        } else if (passwordProvided && rawPassword.length() < MIN_PASSWORD_LENGTH) {
            return ResponseUtility.getErrorResponse("E0103", MessageType.ERROR_PASSWORD_TOO_SHORT);
        }

        String rawRole = dto.getRole();
        UserRoleType parsedRole = parseRole(rawRole);
        if (rawRole != null && !rawRole.isBlank() && parsedRole == null) {
            return ResponseUtility.getErrorResponse("E0105", MessageType.ERROR_INVALID_ROLE);
        }

        // Clash check: case-insensitive via the column collation. Excludes
        // self on update, same shape as ProductServiceImpl.setProduct.
        User clash = userUtility.findByUsername(username);
        if (clash != null && (dto.getId() == null || !clash.getId().equals(dto.getId()))) {
            return ResponseUtility.getErrorResponse("E0104", MessageType.ERROR_USERNAME_ALREADY_EXISTS);
        }

        try {
            User user;
            Date now = new Date();
            String actor = securityUtil.currentUsername();

            if (isCreate) {
                UserRoleType granted = parsedRole != null ? parsedRole : UserRoleType.EMPLOYEE;
                Long roleId = userUtility.findRoleIdByRoleType(granted);
                if (roleId == null) {
                    log.error("Role {} is missing from user_role. Apply sql/tables/user_role.sql.", granted);
                    return ResponseUtility.getErrorResponse("E9999", MessageType.ERROR_SOMETHING_WENT_WRONG);
                }

                user = new User();
                user.setUsername(username);
                user.setPassword(securityUtil.hashPassword(rawPassword));
                user.setName(dto.getName());
                user.setUserRoleId(roleId);
                user.setIsActive(Boolean.TRUE);
                user.setCreatedBy(actor);
                user.setCreatedAt(now);
            } else {
                // Update path: step group B implements this (role-change
                // safeguards, leave-unchanged-when-blank). Group A's tests
                // never reach here (dto.getId() is always null in them).
                throw new UnsupportedOperationException("update path not yet implemented — step group B");
            }

            user.setUpdatedBy(actor);
            user.setUpdatedAt(now);

            return ResponseUtility.getSuccessResponse(
                    MessageType.SUCCESSFULLY_SET_USER, userRepository.save(user));
        } catch (DataIntegrityViolationException e) {
            log.warn("setUser hit a constraint violation for username={}", username);
            return ResponseUtility.getErrorResponse("E0104", MessageType.ERROR_USERNAME_ALREADY_EXISTS);
        } catch (Exception e) {
            log.error("setUser failed for id={}", dto.getId(), e);
            return ResponseUtility.getErrorResponse("E9999", MessageType.ERROR_SOMETHING_WENT_WRONG);
        }
    }

    @Override
    public ResponseDto listUsers(UserListDto dto) {
        throw new UnsupportedOperationException("not yet implemented — step group C");
    }

    @Override
    public ResponseDto getUserById(Long userId) {
        throw new UnsupportedOperationException("not yet implemented — step group C");
    }

    @Override
    public ResponseDto activateUser(Long userId) {
        throw new UnsupportedOperationException("not yet implemented — step group D");
    }

    @Override
    public ResponseDto deactivateUser(Long userId) {
        throw new UnsupportedOperationException("not yet implemented — step group D");
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
}
