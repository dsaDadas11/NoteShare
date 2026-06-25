package com.example.noteshare.service;

import com.example.noteshare.common.BusinessException;
import com.example.noteshare.common.ErrorCode;
import com.example.noteshare.dto.request.LoginRequest;
import com.example.noteshare.dto.request.RegisterRequest;
import com.example.noteshare.dto.response.LoginResponse;
import com.example.noteshare.dto.response.UserResponse;
import com.example.noteshare.entity.User;
import com.example.noteshare.repository.UserRepository;
import com.example.noteshare.security.JwtUtil;
import org.springframework.dao.DataIntegrityViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void register_Success() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("testuser");
        req.setPassword("password123");
        req.setNickname("Test User");

        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");

        UserResponse res = authService.register(req);

        assertNotNull(res);
        assertEquals("testuser", res.getUsername());
        assertEquals("Test User", res.getNickname());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals("testuser", savedUser.getUsername());
        assertEquals("encodedPassword", savedUser.getPasswordHash());
    }

    @Test
    void register_UsernameExists() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("testuser");
        req.setPassword("password123");

        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.register(req));
        assertEquals(ErrorCode.REGISTER_USERNAME_EXISTS, exception.getErrorCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_ConcurrentDuplicateUsername() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("testuser");
        req.setPassword("password123");

        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        doThrow(new DataIntegrityViolationException("duplicate username"))
                .when(userRepository).flush();

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.register(req));

        assertEquals(ErrorCode.REGISTER_USERNAME_EXISTS, exception.getErrorCode());
    }

    @Test
    void login_Success() {
        LoginRequest req = new LoginRequest();
        req.setUsername("testuser");
        req.setPassword("password123");

        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("testuser");
        mockUser.setPasswordHash("encodedPassword");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(jwtUtil.generateToken(1L, "testuser")).thenReturn("mockJwtToken");

        LoginResponse res = authService.login(req);

        assertNotNull(res);
        assertEquals("mockJwtToken", res.getToken());
        assertEquals("testuser", res.getUser().getUsername());
    }

    @Test
    void login_UserNotFound() {
        LoginRequest req = new LoginRequest();
        req.setUsername("nonexistent");
        req.setPassword("password");

        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.login(req));
        assertEquals(ErrorCode.LOGIN_FAILED, exception.getErrorCode());
    }

    @Test
    void login_WrongPassword() {
        LoginRequest req = new LoginRequest();
        req.setUsername("testuser");
        req.setPassword("wrongpass");

        User mockUser = new User();
        mockUser.setPasswordHash("encodedPassword");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("wrongpass", "encodedPassword")).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.login(req));
        assertEquals(ErrorCode.LOGIN_FAILED, exception.getErrorCode());
    }
}
