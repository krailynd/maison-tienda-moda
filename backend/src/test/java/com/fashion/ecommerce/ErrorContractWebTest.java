package com.fashion.ecommerce;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Error-contract web tests (REQ-API-002 / T-S1-006): every {@code /api/v1/**}
 * error returns {@code {"error":{"code","message","details?"}}} with no stack
 * trace, even when no controller maps the route yet (REQ-API-001 gate).
 */
@AutoConfigureMockMvc
class ErrorContractWebTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unknownApiRouteReturnsContractError() throws Exception {
        mockMvc.perform(get("/api/v1/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").isNotEmpty())
                .andExpect(jsonPath("$.error.details").doesNotExist());
    }

    @Test
    void unmappedProductsRouteReturnsContractErrorNotStacktrace() throws Exception {
        mockMvc.perform(get("/api/v1/products/a/b"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.stacktrace").doesNotExist())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.error.details").doesNotExist());
    }
}
