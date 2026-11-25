package com.api.resibo.services.user;

import com.api.resibo.dtos.ResponseDto;
import com.api.resibo.dtos.UserDto;
import com.api.resibo.dtos.UserListDto;

public interface UserService {

    /** Creates when {@code dto.id} is null, updates when it is set — see {@link UserDto}. */
    ResponseDto setUser(UserDto dto);

    /** Filtered, sorted, paginated listing. Data is a {@code PageDto<User>}. */
    ResponseDto listUsers(UserListDto dto);

    /** Single account by id. */
    ResponseDto getUserById(Long userId);

    /** Sets {@code is_active = true}. No safeguard needed — activating cannot strand the system. */
    ResponseDto activateUser(Long userId);

    /**
     * Sets {@code is_active = false}. Refuses to deactivate the caller's own
     * account, and refuses to deactivate the last remaining active ADMIN.
     */
    ResponseDto deactivateUser(Long userId);
}
