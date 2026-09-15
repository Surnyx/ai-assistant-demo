package com.demo.aiassistant.config;

import com.demo.aiassistant.exception.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginInterceptorTest {

    private final LoginInterceptor interceptor = new LoginInterceptor();

    @Test
    void shouldRejectRequestWithoutSession() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThatThrownBy(() -> interceptor.preHandle(
                request, new MockHttpServletResponse(), new Object()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("请先登录");
    }

    @Test
    void shouldAllowLoggedInUser() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute(SessionConstants.USER_ID, 1L);

        boolean allowed = interceptor.preHandle(
                request, new MockHttpServletResponse(), new Object());

        assertThat(allowed).isTrue();
    }
}

