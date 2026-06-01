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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 认证服务：注册 / 登录
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    /**
     * 注册
     */
    public UserResponse register(RegisterRequest req) {
        // 1. 检查用户名是否已存在
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new BusinessException(ErrorCode.REGISTER_USERNAME_EXISTS);
        }

        // 2. 创建用户，密码 BCrypt 加密
        User user = new User();
        user.setUsername(req.getUsername());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setNickname(req.getNickname() != null && !req.getNickname().isBlank()
                ? req.getNickname() : req.getUsername());

        userRepository.save(user);

        return UserResponse.from(user);
    }

    /**
     * 登录
     */
    public LoginResponse login(LoginRequest req) {
        // 1. 查找用户
        User user = userRepository.findByUsername(req.getUsername())
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));

        // 2. 验证密码
        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        // 3. 生成 JWT
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());

        return new LoginResponse(token, UserResponse.from(user));
    }
}
