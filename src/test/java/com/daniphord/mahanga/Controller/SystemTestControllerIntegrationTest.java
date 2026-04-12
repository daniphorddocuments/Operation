package com.daniphord.mahanga.Controller;

import com.daniphord.mahanga.Model.User;
import com.daniphord.mahanga.Repositories.UserRepository;
import com.daniphord.mahanga.Util.OperationRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SystemTestControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void superAdminCanRunSystemVerification() throws Exception {
        User adminUser = userRepository.findByUsername("Mahanga")
                .orElseThrow(() -> new IllegalStateException("Expected seeded super admin account"));

        MvcResult result = mockMvc.perform(post("/api/system-tests/run")
                        .session(sessionFor(adminUser.getId(), adminUser.getUsername(), OperationRole.SUPER_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.status").isString())
                .andExpect(jsonPath("$.totalChecks").isNumber())
                .andExpect(jsonPath("$.summary").isString())
                .andReturn();

        @SuppressWarnings("unchecked")
        Map<String, Object> response = objectMapper.readValue(result.getResponse().getContentAsByteArray(), Map.class);
        Number reportId = (Number) response.get("id");

        mockMvc.perform(get("/api/system-tests/{id}", reportId.longValue())
                        .session(sessionFor(adminUser.getId(), adminUser.getUsername(), OperationRole.SUPER_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Architecture standard alignment")))
                .andExpect(content().string(containsString("Dashboard UX readiness")))
                .andExpect(content().string(containsString("Monitoring and observability")))
                .andExpect(content().string(containsString("Brute-force resilience")))
                .andExpect(content().string(containsString("SQL injection resilience")));
    }

    @Test
    void systemVerificationClearsPythonWarningsWhenLocalPythonIsAvailable() throws Exception {
        Assumptions.assumeTrue(pythonAvailable());

        User adminUser = userRepository.findByUsername("Mahanga")
                .orElseThrow(() -> new IllegalStateException("Expected seeded super admin account"));

        MvcResult result = mockMvc.perform(post("/api/system-tests/run")
                        .session(sessionFor(adminUser.getId(), adminUser.getUsername(), OperationRole.SUPER_ADMIN)))
                .andExpect(status().isOk())
                .andReturn();

        @SuppressWarnings("unchecked")
        Map<String, Object> response = objectMapper.readValue(result.getResponse().getContentAsByteArray(), Map.class);
        Number reportId = (Number) response.get("id");

        mockMvc.perform(get("/api/system-tests/{id}", reportId.longValue())
                        .session(sessionFor(adminUser.getId(), adminUser.getUsername(), OperationRole.SUPER_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Python AI sidecar is not running."))))
                .andExpect(content().string(not(containsString("Python recommendation endpoint is unavailable."))))
                .andExpect(content().string(not(containsString("Python executive system analysis is unavailable."))));
    }

    private MockHttpSession sessionFor(Long userId, String username, String role) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", userId);
        session.setAttribute("username", username);
        session.setAttribute("role", role);
        return session;
    }

    private boolean pythonAvailable() {
        return commandSucceeds("py", "-3", "--version") || commandSucceeds("python", "--version");
    }

    private boolean commandSucceeds(String... command) {
        try {
            Process process = new ProcessBuilder(command).start();
            return process.waitFor() == 0;
        } catch (IOException exception) {
            return false;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
