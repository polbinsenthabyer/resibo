package com.api.resibo.services.user;

import com.api.resibo.dtos.PageDto;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Date;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserUtility userUtility;
    private final SecurityUtil securityUtil;

    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PAGE_SIZE = 200;

    /** Mirrors ProductServiceImpl.SORT_COLUMNS — see that class for why this exists. */
    private static final Map<String, String> SORT_COLUMNS = Map.of(
            "id", "id",
            "username", "username",
            "name", "name",
            "createdAt", "created_at",
            "updatedAt", "updated_at",
            "lastLoginAt", "last_login_at");

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
        // Checked here rather than left to the database, same reasoning as
        // ProductServiceImpl's productDescription check: a column-length
        // violation surfaces as an opaque SQL error, not a usable message.
        if (dto.getName() != null && dto.getName().length() > 100) {
            return ResponseUtility.getErrorResponse("E0204", MessageType.ERROR_NAME_TOO_LONG);
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
                user = userRepository.findById(dto.getId()).orElse(null);
                if (user == null) {
                    return ResponseUtility.getErrorResponse("E0201", MessageType.ERROR_USER_NOT_EXIST);
                }

                UserRoleType currentRole = userUtility.roleTypeOf(user);
                boolean roleIsChanging = parsedRole != null && parsedRole != currentRole;
                if (roleIsChanging
                        && currentRole == UserRoleType.ADMIN
                        && parsedRole == UserRoleType.EMPLOYEE) {
                    if (actor != null && actor.equalsIgnoreCase(user.getUsername())) {
                        return ResponseUtility.getErrorResponse("E0202", MessageType.ERROR_CANNOT_MODIFY_OWN_ACCOUNT);
                    }
                    if (Boolean.TRUE.equals(user.getIsActive())) {
                        Long adminRoleId = userUtility.findRoleIdByRoleType(UserRoleType.ADMIN);
                        if (userRepository.countActiveUsersByRoleId(adminRoleId) <= 1) {
                            return ResponseUtility.getErrorResponse("E0203", MessageType.ERROR_LAST_ACTIVE_ADMIN);
                        }
                    }
                }
                if (roleIsChanging) {
                    user.setUserRoleId(userUtility.findRoleIdByRoleType(parsedRole));
                }
                // else: userRoleId is left exactly as it was — no re-resolve,
                // no reassignment, so a plain rename never needs the current
                // role's id to leave it alone.

                user.setUsername(username);
                user.setName(dto.getName());
                if (passwordProvided) {
                    user.setPassword(securityUtil.hashPassword(rawPassword));
                }
                // isActive, createdBy, createdAt are untouched here on purpose —
                // activation is a dedicated endpoint, not folded into /set.
            }

            user.setUpdatedBy(actor);
            user.setUpdatedAt(now);

            // saveAndFlush, not save: on the update path the entity loaded by
            // findById is already managed, so a plain save() defers the
            // UPDATE to commit-time flush — after this method has already
            // returned — and a unique-constraint violation would surface as
            // an unhandled 500 instead of the E0104 below. Flushing inside
            // the try makes the violation happen, and be caught, here.
            return ResponseUtility.getSuccessResponse(
                    MessageType.SUCCESSFULLY_SET_USER, userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException e) {
            log.warn("setUser hit a constraint violation for username={}", username);
            // Guarded: currentTransactionStatus() throws NoTransactionException
            // outside a real Spring-managed transaction, which is exactly the
            // case in UserServiceImplTest — no Spring context, by design.
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            }
            return ResponseUtility.getErrorResponse("E0104", MessageType.ERROR_USERNAME_ALREADY_EXISTS);
        } catch (Exception e) {
            log.error("setUser failed for id={}", dto.getId(), e);
            // Without this, a managed entity already mutated above (e.g. a
            // role or password change applied before an unrelated failure)
            // would still be flushed at commit — the caller told "it
            // failed" while the database quietly did it anyway.
            // Guarded: currentTransactionStatus() throws NoTransactionException
            // outside a real Spring-managed transaction, which is exactly the
            // case in UserServiceImplTest — no Spring context, by design.
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            }
            return ResponseUtility.getErrorResponse("E9999", MessageType.ERROR_SOMETHING_WENT_WRONG);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseDto listUsers(UserListDto dto) {
        if (dto == null) {
            return ResponseUtility.getErrorResponse("E0001", MessageType.ERROR_DTO_IS_NULL_OR_INCOMPLETE);
        }

        try {
            int page = dto.getPage() == null || dto.getPage() < 0 ? 0 : dto.getPage();
            int size = dto.getSize() == null || dto.getSize() < 1
                    ? 10 : Math.min(dto.getSize(), MAX_PAGE_SIZE);

            // dto.getSortBy() can be an explicit JSON null (Jackson overwrites
            // the field's "id" default), and Map.of(...)'s getOrDefault(null, ..)
            // throws NPE rather than returning the default — guard before the
            // lookup, not inside it.
            String sortColumn = dto.getSortBy() == null
                    ? "id" : SORT_COLUMNS.getOrDefault(dto.getSortBy(), "id");
            Sort.Direction direction = "DESC".equalsIgnoreCase(dto.getSortDirection())
                    ? Sort.Direction.DESC : Sort.Direction.ASC;

            String rawUsername = dto.getUsername() == null ? null : dto.getUsername().trim();
            String username = rawUsername == null || rawUsername.isEmpty() ? null : rawUsername;
            boolean exact = Boolean.TRUE.equals(dto.getIsExactMatch());

            String rawRole = dto.getRole();
            Long userRoleId = null;
            if (rawRole != null && !rawRole.isBlank()) {
                UserRoleType roleFilter = parseRole(rawRole);
                if (roleFilter == null) {
                    // An unparseable filter must not silently become "no
                    // filter" — that would hand back every account,
                    // including admins, to a caller who typoed a role name.
                    return ResponseUtility.getErrorResponse("E0105", MessageType.ERROR_INVALID_ROLE);
                }
                userRoleId = userUtility.findRoleIdByRoleType(roleFilter);
                if (userRoleId == null) {
                    // The role parsed but has no row in user_role — the
                    // schema was not fully applied. Same defensive pattern as
                    // the create path: log it, do not quietly return an
                    // empty or unfiltered page as if the filter worked.
                    log.error("Role {} is missing from user_role. Apply sql/tables/user_role.sql.", roleFilter);
                    return ResponseUtility.getErrorResponse("E9999", MessageType.ERROR_SOMETHING_WENT_WRONG);
                }
            }

            Page<User> result = userRepository.search(
                    exact ? username : null,
                    exact ? null : username,
                    userRoleId,
                    dto.getIsActive(),
                    PageRequest.of(page, size, Sort.by(direction, sortColumn)));

            return ResponseUtility.getSuccessResponse(
                    MessageType.SUCCESSFULLY_FETCHED_DATA, PageDto.of(result));
        } catch (Exception e) {
            log.error("listUsers failed", e);
            return ResponseUtility.getErrorResponse("E9999", MessageType.ERROR_SOMETHING_WENT_WRONG);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseDto getUserById(Long userId) {
        if (userId == null) {
            return ResponseUtility.getErrorResponse("E0001", MessageType.ERROR_DTO_IS_NULL_OR_INCOMPLETE);
        }
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseUtility.getErrorResponse("E0201", MessageType.ERROR_USER_NOT_EXIST);
        }
        return ResponseUtility.getSuccessResponse(MessageType.SUCCESSFULLY_FETCHED_DATA, user);
    }

    @Override
    @Transactional
    public ResponseDto activateUser(Long userId) {
        if (userId == null) {
            return ResponseUtility.getErrorResponse("E0001", MessageType.ERROR_DTO_IS_NULL_OR_INCOMPLETE);
        }
        User user = userUtility.findById(userId);
        if (user == null) {
            return ResponseUtility.getErrorResponse("E0201", MessageType.ERROR_USER_NOT_EXIST);
        }
        try {
            user.setIsActive(Boolean.TRUE);
            user.setUpdatedBy(securityUtil.currentUsername());
            user.setUpdatedAt(new Date());
            userRepository.saveAndFlush(user);
            return ResponseUtility.getSuccessResponse(MessageType.SUCCESSFULLY_ACTIVATED_USER);
        } catch (Exception e) {
            log.error("activateUser failed for id={}", userId, e);
            // Guarded: currentTransactionStatus() throws NoTransactionException
            // outside a real Spring-managed transaction, which is exactly the
            // case in UserServiceImplTest — no Spring context, by design.
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            }
            return ResponseUtility.getErrorResponse("E9999", MessageType.ERROR_SOMETHING_WENT_WRONG);
        }
    }

    @Override
    @Transactional
    public ResponseDto deactivateUser(Long userId) {
        if (userId == null) {
            return ResponseUtility.getErrorResponse("E0001", MessageType.ERROR_DTO_IS_NULL_OR_INCOMPLETE);
        }
        User user = userUtility.findById(userId);
        if (user == null) {
            return ResponseUtility.getErrorResponse("E0201", MessageType.ERROR_USER_NOT_EXIST);
        }

        String caller = securityUtil.currentUsername();
        if (caller != null && caller.equalsIgnoreCase(user.getUsername())) {
            return ResponseUtility.getErrorResponse("E0202", MessageType.ERROR_CANNOT_MODIFY_OWN_ACCOUNT);
        }

        UserRoleType role = userUtility.roleTypeOf(user);
        if (role == UserRoleType.ADMIN && Boolean.TRUE.equals(user.getIsActive())) {
            Long adminRoleId = userUtility.findRoleIdByRoleType(UserRoleType.ADMIN);
            if (userRepository.countActiveUsersByRoleId(adminRoleId) <= 1) {
                return ResponseUtility.getErrorResponse("E0203", MessageType.ERROR_LAST_ACTIVE_ADMIN);
            }
        }

        try {
            user.setIsActive(Boolean.FALSE);
            user.setUpdatedBy(caller);
            user.setUpdatedAt(new Date());
            userRepository.saveAndFlush(user);
            return ResponseUtility.getSuccessResponse(MessageType.SUCCESSFULLY_DEACTIVATED_USER);
        } catch (Exception e) {
            log.error("deactivateUser failed for id={}", userId, e);
            // Guarded: currentTransactionStatus() throws NoTransactionException
            // outside a real Spring-managed transaction, which is exactly the
            // case in UserServiceImplTest — no Spring context, by design.
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            }
            return ResponseUtility.getErrorResponse("E9999", MessageType.ERROR_SOMETHING_WENT_WRONG);
        }
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
