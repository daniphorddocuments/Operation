package com.daniphord.mahanga.Controller;

import com.daniphord.mahanga.Model.User;
import com.daniphord.mahanga.Repositories.UserRepository;
import com.daniphord.mahanga.Util.OperationRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import jakarta.servlet.http.Cookie;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "froms.security.csrf-enabled=true")
@AutoConfigureMockMvc
class LiveCsrfIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void dashboardRenderPublishesCsrfMetaTagsForAuthenticatedSession() throws Exception {
        User adminUser = userRepository.findByUsername("csrf.dashboard.admin")
                .orElseGet(() -> createAdminUser("csrf.dashboard.admin"));

        mockMvc.perform(get("/dashboard")
                        .session(sessionFor(adminUser.getId(), adminUser.getUsername(), adminUser.getRole())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("meta name=\"_csrf\"")))
                .andExpect(content().string(containsString("meta name=\"_csrf_header\"")));
    }

    @Test
    void csrfProtectedAdminPostActionsWorkWhenSecurityTokensArePresent() throws Exception {
        User adminUser = userRepository.findByUsername("csrf.post.admin")
                .orElseGet(() -> createAdminUser("csrf.post.admin"));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "csrf-flow-user",
                                  "checkNumber": "csrf-flow-user",
                                  "fullName": "CSRF Flow User",
                                  "email": "csrf-flow@example.com",
                                  "role": "UNASSIGNED",
                                  "password": "StrongPassword123!"
                                }
                                """)
                        .session(sessionFor(adminUser.getId(), adminUser.getUsername(), adminUser.getRole()))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("csrf-flow-user"));

        mockMvc.perform(post("/api/system-tests/run")
                        .session(sessionFor(adminUser.getId(), adminUser.getUsername(), adminUser.getRole()))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    void dashboardIssuedCookieTokenWorksForAjaxPostActions() throws Exception {
        User adminUser = userRepository.findByUsername("csrf.cookie.admin")
                .orElseGet(() -> createAdminUser("csrf.cookie.admin"));

        MvcResult dashboardResult = mockMvc.perform(get("/dashboard")
                        .session(sessionFor(adminUser.getId(), adminUser.getUsername(), adminUser.getRole())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("meta name=\"_csrf\"")))
                .andReturn();

        Cookie csrfCookie = dashboardResult.getResponse().getCookie("XSRF-TOKEN");
        org.junit.jupiter.api.Assertions.assertNotNull(csrfCookie);
        org.junit.jupiter.api.Assertions.assertNotNull(csrfCookie.getValue());
        org.junit.jupiter.api.Assertions.assertFalse(csrfCookie.getValue().isBlank());

        mockMvc.perform(post("/api/system-tests/run")
                        .session(sessionFor(adminUser.getId(), adminUser.getUsername(), adminUser.getRole()))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber());
    }

    private MockHttpSession sessionFor(Long userId, String username, String role) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", userId);
        session.setAttribute("username", username);
        session.setAttribute("role", role);
        return session;
    }

    private User createAdminUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode("changeMeNow123!"));
        user.setRole(OperationRole.ADMIN);
        user.setFullName("CSRF Admin");
        user.setDesignation(OperationRole.ADMIN);
        return userRepository.save(user);
    }
}
