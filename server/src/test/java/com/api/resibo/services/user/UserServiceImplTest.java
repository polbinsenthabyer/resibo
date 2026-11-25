package com.api.resibo.services.user;

import com.api.resibo.dtos.UserDto;
import com.api.resibo.dtos.ResponseDto;
import com.api.resibo.entities.User;
import com.api.resibo.repositories.UserRepository;
import com.api.resibo.types.UserRoleType;
import com.api.resibo.utilities.SecurityUtil;
import com.api.resibo.utilities.UserUtility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private UserUtility userUtility;
    @Mock private SecurityUtil securityUtil;
    @InjectMocks private UserServiceImpl service;

    @Test
    void setRejectsNullDto() {
        ResponseDto res = service.setUser(null);
        assertThat(res.getCode()).isEqualTo("E0001");
        verify(userRepository, never()).save(any());
    }

    @Test
    void setRejectsBlankUsername() {
        UserDto dto = new UserDto();
        dto.setUsername("   ");
        assertThat(service.setUser(dto).getCode()).isEqualTo("E0101");
    }

    @Test
    void setRejectsUsernameLongerThanTheColumn() {
        UserDto dto = new UserDto();
        dto.setUsername("x".repeat(101));
        assertThat(service.setUser(dto).getCode()).isEqualTo("E0102");
    }

    @Test
    void setRejectsCreateWithoutPassword() {
        UserDto dto = new UserDto();
        dto.setUsername("newhire");
        assertThat(service.setUser(dto).getCode()).isEqualTo("E0103");
    }

    @Test
    void setRejectsCreateWithShortPassword() {
        UserDto dto = new UserDto();
        dto.setUsername("newhire");
        dto.setPassword("short");
        assertThat(service.setUser(dto).getCode()).isEqualTo("E0103");
    }

    @Test
    void setRejectsInvalidRole() {
        UserDto dto = new UserDto();
        dto.setUsername("newhire");
        dto.setPassword("password123");
        dto.setRole("OWNER");
        assertThat(service.setUser(dto).getCode()).isEqualTo("E0105");
    }

    @Test
    void setRejectsDuplicateUsernameOnCreate() {
        User existing = new User();
        existing.setId(1L);
        existing.setUsername("taken");
        when(userUtility.findByUsername("taken")).thenReturn(existing);

        UserDto dto = new UserDto();
        dto.setUsername("taken");
        dto.setPassword("password123");

        assertThat(service.setUser(dto).getCode()).isEqualTo("E0104");
        verify(userRepository, never()).save(any());
    }

    @Test
    void setCreateDefaultsRoleToEmployeeHashesPasswordAndSetsActiveTrue() {
        when(securityUtil.currentUsername()).thenReturn("polbinsent");
        when(securityUtil.hashPassword("password123")).thenReturn("HASHED");
        when(userUtility.findRoleIdByRoleType(UserRoleType.EMPLOYEE)).thenReturn(2L);
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UserDto dto = new UserDto();
        dto.setUsername("  newhire  ");
        dto.setPassword("password123");

        ResponseDto res = service.setUser(dto);
        assertThat(res.isSuccess()).isTrue();

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        User u = saved.getValue();
        assertThat(u.getUsername()).isEqualTo("newhire");
        assertThat(u.getPassword()).isEqualTo("HASHED");
        assertThat(u.getUserRoleId()).isEqualTo(2L);
        assertThat(u.getIsActive()).isTrue();
        assertThat(u.getCreatedBy()).isEqualTo("polbinsent");
        assertThat(u.getCreatedAt()).isNotNull();
    }

    @Test
    void setCreateHonoursExplicitAdminRole() {
        when(securityUtil.hashPassword(any())).thenReturn("HASHED");
        when(userUtility.findRoleIdByRoleType(UserRoleType.ADMIN)).thenReturn(1L);
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UserDto dto = new UserDto();
        dto.setUsername("newadmin");
        dto.setPassword("password123");
        dto.setRole("admin");

        service.setUser(dto);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getUserRoleId()).isEqualTo(1L);
    }
}
