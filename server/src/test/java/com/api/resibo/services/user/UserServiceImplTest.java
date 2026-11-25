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

    // --- setUser: update, role-change safeguards, and the duplicate race --

    @Test
    void setUpdateLeavesRoleUnchangedWhenBlank() {
        User existing = new User();
        existing.setId(5L);
        existing.setUsername("staffer");
        existing.setUserRoleId(2L);
        existing.setIsActive(true);
        when(userRepository.findById(5L)).thenReturn(java.util.Optional.of(existing));
        when(userUtility.roleTypeOf(existing)).thenReturn(UserRoleType.EMPLOYEE);
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UserDto dto = new UserDto();
        dto.setId(5L);
        dto.setUsername("staffer");   // unchanged
        dto.setName("Renamed Person");

        assertThat(service.setUser(dto).isSuccess()).isTrue();

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getUserRoleId()).isEqualTo(2L);   // unchanged, not reset
        assertThat(saved.getValue().getIsActive()).isTrue();          // untouched by /set
    }

    @Test
    void setUpdateWithoutPasswordLeavesHashUnchanged() {
        User existing = new User();
        existing.setId(6L);
        existing.setUsername("staffer");
        existing.setPassword("OLD_HASH");
        when(userRepository.findById(6L)).thenReturn(java.util.Optional.of(existing));
        when(userUtility.roleTypeOf(existing)).thenReturn(UserRoleType.EMPLOYEE);
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UserDto dto = new UserDto();
        dto.setId(6L);
        dto.setUsername("staffer");

        service.setUser(dto);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getPassword()).isEqualTo("OLD_HASH");
        verify(securityUtil, never()).hashPassword(any());
    }

    @Test
    void setRejectsCaseInsensitiveDuplicateOnRename() {
        // The clash check runs before the fetch-by-id (same order as
        // ProductServiceImpl.setProduct), so it returns before findById is
        // ever called — no stub for the existing row is needed here.
        User clash = new User();
        clash.setId(9L);
        clash.setUsername("pol");
        when(userUtility.findByUsername("POL")).thenReturn(clash);

        UserDto dto = new UserDto();
        dto.setId(5L);
        dto.setUsername("POL");   // different case, same row as "pol"

        assertThat(service.setUser(dto).getCode()).isEqualTo("E0104");
        verify(userRepository, never()).save(any());
    }

    @Test
    void setWithUnknownIdReturnsNotExist() {
        when(userRepository.findById(404L)).thenReturn(java.util.Optional.empty());
        UserDto dto = new UserDto();
        dto.setId(404L);
        dto.setUsername("ghost");

        assertThat(service.setUser(dto).getCode()).isEqualTo("E0201");
        verify(userRepository, never()).save(any());
    }

    @Test
    void setRejectsSelfDemotion() {
        User admin = new User();
        admin.setId(1L);
        admin.setUsername("polbinsent");
        admin.setIsActive(true);
        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(admin));
        when(userUtility.roleTypeOf(admin)).thenReturn(UserRoleType.ADMIN);
        when(securityUtil.currentUsername()).thenReturn("polbinsent");

        UserDto dto = new UserDto();
        dto.setId(1L);
        dto.setUsername("polbinsent");
        dto.setRole("EMPLOYEE");

        assertThat(service.setUser(dto).getCode()).isEqualTo("E0202");
        verify(userRepository, never()).save(any());
    }

    @Test
    void setRejectsDemotingTheLastActiveAdmin() {
        User otherAdmin = new User();
        otherAdmin.setId(2L);
        otherAdmin.setUsername("other-admin");
        otherAdmin.setIsActive(true);
        when(userRepository.findById(2L)).thenReturn(java.util.Optional.of(otherAdmin));
        when(userUtility.roleTypeOf(otherAdmin)).thenReturn(UserRoleType.ADMIN);
        when(securityUtil.currentUsername()).thenReturn("polbinsent");   // different caller
        when(userUtility.findRoleIdByRoleType(UserRoleType.ADMIN)).thenReturn(1L);
        when(userRepository.countActiveUsersByRoleId(1L)).thenReturn(1L);   // only this one

        UserDto dto = new UserDto();
        dto.setId(2L);
        dto.setUsername("other-admin");
        dto.setRole("EMPLOYEE");

        assertThat(service.setUser(dto).getCode()).isEqualTo("E0203");
        verify(userRepository, never()).save(any());
    }

    @Test
    void setAllowsDemotingAnAdminWhenAnotherActiveAdminExists() {
        User otherAdmin = new User();
        otherAdmin.setId(2L);
        otherAdmin.setUsername("other-admin");
        otherAdmin.setIsActive(true);
        when(userRepository.findById(2L)).thenReturn(java.util.Optional.of(otherAdmin));
        when(userUtility.roleTypeOf(otherAdmin)).thenReturn(UserRoleType.ADMIN);
        when(securityUtil.currentUsername()).thenReturn("polbinsent");
        when(userUtility.findRoleIdByRoleType(UserRoleType.ADMIN)).thenReturn(1L);
        when(userRepository.countActiveUsersByRoleId(1L)).thenReturn(2L);   // two active admins
        when(userUtility.findRoleIdByRoleType(UserRoleType.EMPLOYEE)).thenReturn(2L);
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UserDto dto = new UserDto();
        dto.setId(2L);
        dto.setUsername("other-admin");
        dto.setRole("EMPLOYEE");

        assertThat(service.setUser(dto).isSuccess()).isTrue();
    }

    @Test
    void setTranslatesAConstraintViolationIntoTheDuplicateCodeOnUpdate() {
        User existing = new User();
        existing.setId(5L);
        existing.setUsername("staffer");
        when(userUtility.findByUsername("renamed")).thenReturn(null);
        when(userRepository.findById(5L)).thenReturn(java.util.Optional.of(existing));
        when(userUtility.roleTypeOf(existing)).thenReturn(UserRoleType.EMPLOYEE);
        when(userRepository.save(any(User.class)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("duplicate"));

        UserDto dto = new UserDto();
        dto.setId(5L);
        dto.setUsername("renamed");

        assertThat(service.setUser(dto).getCode()).isEqualTo("E0104");
    }

    // --- listUsers: pagination, sorting, role filter -----------------------

    @Test
    void listClampsPageSizeAndFallsBackToIdForUnknownSort() {
        when(userRepository.search(any(), any(), any(), any(), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(
                        java.util.List.of(),
                        org.springframework.data.domain.PageRequest.of(0, 200), 0));

        com.api.resibo.dtos.UserListDto dto = new com.api.resibo.dtos.UserListDto();
        dto.setSize(10_000);
        dto.setPage(-5);
        dto.setSortBy("; DROP TABLE user");

        assertThat(service.listUsers(dto).isSuccess()).isTrue();

        ArgumentCaptor<org.springframework.data.domain.Pageable> pageable =
                ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);
        verify(userRepository).search(any(), any(), any(), any(), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(200);
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getSort().getOrderFor("id")).isNotNull();
    }

    @Test
    void listResolvesRoleNameToRoleIdFilter() {
        when(userUtility.findRoleIdByRoleType(UserRoleType.ADMIN)).thenReturn(1L);
        when(userRepository.search(any(), any(), any(), any(), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(
                        java.util.List.of(),
                        org.springframework.data.domain.PageRequest.of(0, 10), 0));

        com.api.resibo.dtos.UserListDto dto = new com.api.resibo.dtos.UserListDto();
        dto.setRole("admin");
        service.listUsers(dto);

        verify(userRepository).search(any(), any(),
                org.mockito.ArgumentMatchers.eq(1L), any(),
                any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    void getUnknownIdReturnsNotExist() {
        when(userRepository.findById(1L)).thenReturn(java.util.Optional.empty());
        assertThat(service.getUserById(1L).getCode()).isEqualTo("E0201");
    }

    @Test
    void getExistingReturnsTheUser() {
        User u = new User();
        u.setId(1L);
        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(u));
        ResponseDto res = service.getUserById(1L);
        assertThat(res.isSuccess()).isTrue();
        assertThat(res.getData()).isEqualTo(u);
    }
}
