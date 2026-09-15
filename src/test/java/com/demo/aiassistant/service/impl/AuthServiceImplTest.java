package com.demo.aiassistant.service.impl;

import com.demo.aiassistant.dto.LoginRequest;
import com.demo.aiassistant.dto.LoginResponse;
import com.demo.aiassistant.entity.User;
import com.demo.aiassistant.exception.UnauthorizedException;
import com.demo.aiassistant.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceImplTest {

    private final UserMapper userMapper = mock(UserMapper.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final AuthServiceImpl authService = new AuthServiceImpl(userMapper, passwordEncoder);

    @Test
    void shouldReturnUserWhenPasswordIsCorrect() {
        User user = new User();
        user.setId(1L);
        user.setUsername("test");
        user.setPassword("bcrypt-hash");
        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches("123456", "bcrypt-hash")).thenReturn(true);

        LoginResponse response = authService.login(new LoginRequest("test", "123456"));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.username()).isEqualTo("test");
    }

    @Test
    void shouldRejectWrongPassword() {
        User user = new User();
        user.setPassword("bcrypt-hash");
        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches(any(), any())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("test", "wrong")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("用户名或密码错误");
    }
}

