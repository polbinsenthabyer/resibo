package com.api.resibo.controllers.admin;

import com.api.resibo.dtos.ResponseDto;
import com.api.resibo.dtos.UserDto;
import com.api.resibo.dtos.UserListDto;
import com.api.resibo.services.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-only account management for ADMIN and EMPLOYEE users.
 *
 * <p>Create and update are one operation — {@code /set} — same convention as
 * {@link AdminProductController}. There is no hard delete: activation state
 * is a dedicated pair of endpoints rather than folded into {@code /set}, so
 * it can never be silently reset by an unrelated profile edit.
 *
 * <p>Mounted under {@code /api/admin/**}, which {@code SecurityConfig}
 * restricts to the ADMIN role.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("api/admin/users")
public class AdminUserController {

    private final UserService userService;

    @PostMapping("/set")
    public ResponseDto setUser(@RequestBody UserDto dto) {
        return userService.setUser(dto);
    }

    @PostMapping("/list")
    public ResponseDto listUsers(@RequestBody UserListDto dto) {
        return userService.listUsers(dto);
    }

    @GetMapping("/get/{userId}")
    public ResponseDto getUserById(@PathVariable Long userId) {
        return userService.getUserById(userId);
    }

    @PostMapping("/activate/{userId}")
    public ResponseDto activateUser(@PathVariable Long userId) {
        return userService.activateUser(userId);
    }

    @PostMapping("/deactivate/{userId}")
    public ResponseDto deactivateUser(@PathVariable Long userId) {
        return userService.deactivateUser(userId);
    }
}
