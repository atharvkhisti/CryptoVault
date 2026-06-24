package com.cryptovault.gateway.config;

import com.cryptovault.gateway.GatewayApplication;
import com.cryptovault.gateway.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <h3>SecurityConfigTest</h3>
 *
 * <p><b>Why it exists:</b> Performs integration validation on the configured {@link SecurityConfig} filter chain, asserting route authorization constraints.</p>
 * <p><b>Architectural Layer:</b> Testing Layer.</p>
 * <p><b>Design Patterns Used:</b> Integration Test Pattern (MockMvc).</p>
 * <p><b>Security Concepts Demonstrated:</b> Boundary authentication enforcement, public route exclusion, and stateless security containment.</p>
 * <p><b>Enterprise Relevance:</b> Prevents regression changes that could mistakenly expose private banking/wallet routes to unauthorized public requests.</p>
 * <p><b>Interview Talking Points:</b> Uses <code>MockMvc</code> and <code>@SpringBootTest</code> to load the gateway web context. We assert that public routes (like login/register) are allowed to bypass the gateway's auth check, while private business resources return HTTP 401.</p>
 */
@SpringBootTest(classes = GatewayApplication.class)
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void shouldPermitLoginEndpoint() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@test.com\",\"password\":\"Pass123\"}"))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()));
    }

    @Test
    void shouldPermitRegisterEndpoint() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@test.com\",\"password\":\"Pass123\"}"))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()));
    }

    @Test
    void shouldProtectWalletEndpoint() throws Exception {
        mockMvc.perform(get("/api/wallets/my-wallet")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
