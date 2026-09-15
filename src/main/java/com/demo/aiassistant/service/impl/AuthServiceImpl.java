package com.demo.aiassistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.aiassistant.dto.LoginRequest;
import com.demo.aiassistant.dto.LoginResponse;
import com.demo.aiassistant.entity.User;
import com.demo.aiassistant.exception.UnauthorizedException;
import com.demo.aiassistant.mapper.UserMapper;
import com.demo.aiassistant.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.username()));

        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new UnauthorizedException("用户名或密码错误");
        }

        return new LoginResponse(user.getId(), user.getUsername());
    }
}

