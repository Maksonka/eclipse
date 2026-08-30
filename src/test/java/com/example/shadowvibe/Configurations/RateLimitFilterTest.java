package com.example.shadowvibe.Configurations;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class RateLimitFilterTest {

    private RateLimitFilter filter;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new RateLimitFilter();
        chain = mock(FilterChain.class);
    }

    private int perform(String method, String path) throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest(method, path);
        req.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        filter.doFilter(req, resp, chain);
        return resp.getStatus();
    }

    @Test
    void login_allowsUpToLimitThenRejects() throws Exception {
        for (int i = 0; i < 10; i++) {
            assertEquals(200, perform("POST", "/api/auth/login"), "request #" + (i + 1));
        }
        assertEquals(429, perform("POST", "/api/auth/login"));
        verify(chain, times(10)).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void register_allowsThreeThenRejects() throws Exception {
        for (int i = 0; i < 3; i++) {
            assertEquals(200, perform("POST", "/register"));
        }
        assertEquals(429, perform("POST", "/register"));
    }

    @Test
    void registerCountedSeparatelyFromLogin() throws Exception {
        for (int i = 0; i < 10; i++) {
            perform("POST", "/api/auth/login");
        }
        assertEquals(200, perform("POST", "/register"));
    }

    @Test
    void formLoginPostIsLimited() throws Exception {
        for (int i = 0; i < 10; i++) {
            assertEquals(200, perform("POST", "/login"), "request #" + (i + 1));
        }
        assertEquals(429, perform("POST", "/login"));
        verify(chain, times(10)).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void getRequestsNotLimited() throws Exception {
        for (int i = 0; i < 30; i++) {
            assertEquals(200, perform("GET", "/login"));
        }
        verify(chain, times(30)).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void loginRejectReturnsJson429() throws Exception {
        for (int i = 0; i < 10; i++) {
            perform("POST", "/api/auth/login");
        }
        MockHttpServletResponse resp = new MockHttpServletResponse();
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
        req.setRemoteAddr("127.0.0.1");
        filter.doFilter(req, resp, chain);
        assertEquals(429, resp.getStatus());
        assertEquals("application/json;charset=UTF-8", resp.getContentType());
    }

    @Test
    void differentIpsLimitedIndependently() throws Exception {
        MockHttpServletRequest req1 = new MockHttpServletRequest("POST", "/api/auth/login");
        req1.setRemoteAddr("1.1.1.1");
        MockHttpServletResponse resp1 = new MockHttpServletResponse();
        for (int i = 0; i < 10; i++) {
            filter.doFilter(req1, resp1, chain);
        }
        // 11-я попытка с 1.1.1.1 — лимит исчерпан
        MockHttpServletResponse resp11 = new MockHttpServletResponse();
        filter.doFilter(req1, resp11, chain);
        assertEquals(429, resp11.getStatus());
        // другой IP не затронут
        assertEquals(200, perform("POST", "/api/auth/login"));
    }

    @Test
    void blankForwardedHeaderFallsBackToRemoteAddr() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
        req.setRemoteAddr("127.0.0.1");
        req.addHeader("X-Forwarded-For", "  ");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        for (int i = 0; i < 10; i++) {
            filter.doFilter(req, resp, chain);
        }
        assertEquals(429, perform("POST", "/api/auth/login")); // тот же remoteAddr 127.0.0.1
    }
}
