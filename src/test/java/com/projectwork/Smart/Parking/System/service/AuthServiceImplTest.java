package com.projectwork.Smart.Parking.System.service;

import com.projectwork.Smart.Parking.System.dto.request.LoginRequestDto;
import com.projectwork.Smart.Parking.System.entity.User;
import com.projectwork.Smart.Parking.System.entity.UserRole;
import com.projectwork.Smart.Parking.System.repository.BlacklistedTokenRepository;
import com.projectwork.Smart.Parking.System.repository.RefreshTokenRepository;
import com.projectwork.Smart.Parking.System.repository.UserRepository;
import com.projectwork.Smart.Parking.System.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private BlacklistedTokenRepository blacklistedTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                userRepository,
                refreshTokenRepository,
                blacklistedTokenRepository,
                passwordEncoder,
                jwtUtil);
    }

    @Test
    void loginUser_shouldRejectBannedUserEvenWhenPasswordMatches() {
        User bannedUser = new User();
        ReflectionTestUtils.setField(bannedUser, "id", UUID.randomUUID());
        bannedUser.setName("Banned Driver");
        bannedUser.setEmail("driver@example.com");
        bannedUser.setPassword("encoded-password");
        bannedUser.setPhone("9800000000");
        bannedUser.setRole(UserRole.DRIVER);
        bannedUser.setApproved(true);
        bannedUser.setBanned(true);

        LoginRequestDto request = new LoginRequestDto();
        request.setEmail(" DRIVER@example.com ");
        request.setPassword("correct-password");

        when(userRepository.findByEmailAndDeletedAtIsNull("driver@example.com")).thenReturn(Optional.of(bannedUser));
        when(passwordEncoder.matches("correct-password", "encoded-password")).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.loginUser(request));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(jwtUtil, never()).generateAccessToken(any(), any(), any());
        verify(refreshTokenRepository, never()).save(any());
    }
}
