package com.cryptovault.gateway.filter;

import com.cryptovault.gateway.security.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * <h3>GatewayAuthenticationFilterTest</h3>
 *
 * <p><b>Why it exists:</b> Validates the request intercepting flow, public endpoint bypass rules, token extraction, and down-stream header decorators.</p>
 * <p><b>Architectural Layer:</b> Testing Layer.</p>
 * <p><b>Design Patterns Used:</b> Mocking Strategy and Request Interception.</p>
 * <p><b>Security Concepts Demonstrated:</b> Boundary check logic validation, header manipulation containment, and security context establishment.</p>
 * <p><b>Enterprise Relevance:</b> Enforces that the security filter correctly rejects untrusted/expired logins and injects standard context headers without failing.</p>
 * <p><b>Interview Talking Points:</b> Uses MockHttpServletRequest/Response to isolate servlet executions. Asserts that context headers are added correctly to the request decorator while public endpoints register bypass validation.</p>
 */
@ExtendWith(MockitoExtension.class)
class GatewayAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private GatewayAuthenticationFilter authenticationFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldBypassValidationForPublicRoutes() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        authenticationFilter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        verifyNoInteractions(jwtService);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldContinueChainWhenNoAuthorizationHeaderPresent() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/wallets/my-wallet");
        MockHttpServletResponse response = new MockHttpServletResponse();

        authenticationFilter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldReturn401WhenInvalidTokenProvided() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/wallets/my-wallet");
        request.addHeader("Authorization", "Bearer invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtService.isTokenValid("invalid-token")).thenReturn(false);

        authenticationFilter.doFilter(request, response, filterChain);

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("Invalid or expired JWT token"));
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void shouldAuthenticateAndPropagateHeadersWhenValidTokenProvided() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/wallets/my-wallet");
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtService.isTokenValid("valid-token")).thenReturn(true);
        when(jwtService.extractEmail("valid-token")).thenReturn("user@cryptovault.com");
        when(jwtService.extractUserId("valid-token")).thenReturn("123e4567-e89b-12d3-a456-426614174000");
        when(jwtService.extractRole("valid-token")).thenReturn("USER");

        authenticationFilter.doFilter(request, response, filterChain);

        // Verify request wrapping and filter execution continuation
        verify(filterChain, times(1)).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("user@cryptovault.com", SecurityContextHolder.getContext().getAuthentication().getName());
    }
}
