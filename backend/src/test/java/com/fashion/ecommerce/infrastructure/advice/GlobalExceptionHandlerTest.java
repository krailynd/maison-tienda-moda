package com.fashion.ecommerce.infrastructure.advice;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the canonical {@link GlobalExceptionHandler} (T-S1-006 /
 * REQ-API-002): every error maps to the {@code {error:{code,message,details?}}}
 * contract without leaking stack traces.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @SuppressWarnings("unchecked")
    private Map<String, Object> errorBody(ResponseEntity<Map<String, Object>> response) {
        assertThat(response.getStatusCode().isError()).isTrue();
        return (Map<String, Object>) response.getBody().get("error");
    }

    @Test
    void genericExceptionReturnsContractWithoutStacktrace() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleGenericException(new IllegalStateException("boom"));

        Map<String, Object> error = errorBody(response);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(error.get("code")).isEqualTo("INTERNAL_ERROR");
        assertThat(error.get("message")).isInstanceOf(String.class);
        assertThat(error.get("details")).isNull();
        assertThat(response.getBody().toString()).doesNotContain("at com.fashion");
    }

    @Test
    void accessDeniedMapsTo403() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleAccessDeniedException(new AccessDeniedException("nope"));

        Map<String, Object> error = errorBody(response);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(error.get("code")).isEqualTo("ACCESS_DENIED");
    }

    @Test
    void unknownRouteMapsTo404Contract() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleNoResourceFoundException(new NoResourceFoundException(HttpMethod.GET, "/api/v1/x"));

        Map<String, Object> error = errorBody(response);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(error.get("code")).isEqualTo("NOT_FOUND");
    }
}
