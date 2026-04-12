package com.daniphord.mahanga.Controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApplicationRuntimePerformanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicEntryPointsRespondWithinOperationalBudget() throws Exception {
        long totalMillis = measure("/login") + measure("/public/emergency/report");
        assertTrue(totalMillis < 6000, "Public entry points exceeded the smoke-test performance budget: " + totalMillis + " ms");
    }

    private long measure(String path) throws Exception {
        long startedAt = System.nanoTime();
        mockMvc.perform(get(path))
                .andExpect(status().isOk());
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
