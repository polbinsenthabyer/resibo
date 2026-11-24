package com.api.resibo.services.auth;

import com.api.resibo.dtos.LoginDto;
import com.api.resibo.dtos.ResponseDto;
import com.api.resibo.dtos.SignupDto;

public interface AuthService {

    /**
     * Registers an account. See {@link SignupDto#getRole()} for when a
     * requested role is honoured and when it is downgraded to EMPLOYEE.
     */
    ResponseDto signup(SignupDto dto);

    /** Exchanges credentials for a JWT. Data is a {@code LoginResponseDto}. */
    ResponseDto login(LoginDto dto);
}
