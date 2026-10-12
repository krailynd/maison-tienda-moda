package com.fashion.ecommerce.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the canonical {@link RateLimitFilter} (T-S1-005 / REQ-API-003):
 * the 101st request in the same minute from the same IP is rejected with
 * {@code 429} plus a {@code Retry-After} header and the error contract body.
 */
class RateLimitFilterTest {

    private static final String IP = "192.168.1.10";

    @Test
    void allowsFirstHundredRequestsPerIp() throws Exception {
        RateLimitFilter filter = new RateLimitFilter();

        for (int i = 0; i < 100; i++) {
            MockHttpServletRequest request = requestWithIp();
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, new MockFilterChain());
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    @Test
    void rejectsOverBudgetRequestWith429AndRetryAfter() throws Exception {
        RateLimitFilter filter = new RateLimitFilter();

        for (int i = 0; i < 100; i++) {
            filter.doFilter(requestWithIp(), new MockHttpServletResponse(), new MockFilterChain());
        }

        MockHttpServletRequest overBudget = requestWithIp();
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(overBudget, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isNotBlank();
        assertThat(response.getContentAsString())
                .contains("\"code\":\"RATE_LIMITED\"")
                .contains("\"error\"");
    }

    @Test
    void differentIpsHaveIndependentBuckets() throws Exception {
        RateLimitFilter filter = new RateLimitFilter();

        for (int i = 0; i < 100; i++) {
            filter.doFilter(requestWithIp(), new MockHttpServletResponse(), new MockFilterChain());
        }

        MockHttpServletRequest otherIp = requestWithIp("10.0.0.99");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(otherIp, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
    }

    private MockHttpServletRequest requestWithIp() {
        return requestWithIp(IP);
    }

    private MockHttpServletRequest requestWithIp(String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/products");
        request.setRemoteAddr(ip);
        return request;
    }
}
