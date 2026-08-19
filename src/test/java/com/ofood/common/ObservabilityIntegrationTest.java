package com.ofood.common;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ObservabilityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testRequestLoggingFilterAndTraceId() throws Exception {
        // Send a request and check if traceId is returned in response header or body if it's an error
        mockMvc.perform(get("/api/v1/plans"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Trace-Id"));
    }

    @Test
    void testGlobalExceptionHandlerReturnsTraceIdOnUnexpectedError() throws Exception {
        // We know POST /api/v1/checkout/preview without body will throw a Bad Request (or similar)
        // Let's force a 400 Bad Request to see if traceId is in the error response body
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType("application/json")
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.traceId").exists());
    }
}
