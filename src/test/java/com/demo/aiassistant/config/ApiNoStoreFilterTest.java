package com.demo.aiassistant.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class ApiNoStoreFilterTest {

    private final ApiNoStoreFilter filter = new ApiNoStoreFilter();

    @Test
    void shouldDisableBrowserCachingForApiResponses() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/conversations");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader("Cache-Control"))
                .isEqualTo("no-store, no-cache, must-revalidate, max-age=0");
        assertThat(response.getHeader("Pragma")).isEqualTo("no-cache");
        assertThat(response.getHeader("Expires")).isEqualTo("0");
    }

    @Test
    void shouldLeaveStaticResourceCachingUnchanged() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/css/style.css");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader("Cache-Control")).isNull();
    }
}
