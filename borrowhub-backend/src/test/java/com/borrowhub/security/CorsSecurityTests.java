package com.borrowhub.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CorsSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SecurityConfig securityConfig;

    @Test
    @DisplayName("CORS: Allowed frontend origin receives Access-Control-Allow-Origin and credentials headers")
    void testAllowedFrontendOrigin() throws Exception {
        String allowedOrigin = "http://localhost:5173";

        // Preflight OPTIONS check
        mockMvc.perform(options("/api/resources")
                        .header(HttpHeaders.ORIGIN, allowedOrigin)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, allowedOrigin))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));

        // Simple GET check
        mockMvc.perform(get("/api/resources")
                        .header(HttpHeaders.ORIGIN, allowedOrigin))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, allowedOrigin))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    @DisplayName("CORS: Unknown or malicious origin is rejected with 403 Forbidden and no allow-origin header")
    void testRejectedUnknownOrigin() throws Exception {
        String rejectedOrigin = "https://malicious-attacker.com";

        // Preflight OPTIONS check: Spring Security denies unauthorized CORS preflights with 403
        mockMvc.perform(options("/api/resources")
                        .header(HttpHeaders.ORIGIN, rejectedOrigin)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));

        // Actual GET request: Origin is not in allowed list, so Access-Control-Allow-Origin header is omitted
        mockMvc.perform(get("/api/resources")
                        .header(HttpHeaders.ORIGIN, rejectedOrigin))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    @DisplayName("CORS: Missing production CORS configuration throws IllegalStateException at startup")
    void testMissingProductionCorsConfigurationThrowsException() {
        // 1. Missing / empty origins in production profile
        IllegalStateException ex1 = assertThrows(IllegalStateException.class, () ->
                SecurityConfig.resolveAllowedOrigins(new String[0], true)
        );
        assertTrue(ex1.getMessage().contains("CORS_ALLOWED_ORIGINS is required"));

        IllegalStateException ex2 = assertThrows(IllegalStateException.class, () ->
                SecurityConfig.resolveAllowedOrigins(null, true)
        );
        assertTrue(ex2.getMessage().contains("CORS_ALLOWED_ORIGINS is required"));

        IllegalStateException ex3 = assertThrows(IllegalStateException.class, () ->
                SecurityConfig.resolveAllowedOrigins(new String[]{"  ", ""}, true)
        );
        assertTrue(ex3.getMessage().contains("CORS_ALLOWED_ORIGINS is required"));

        // 2. Production profile containing only localhost origins must be rejected
        IllegalStateException exLocal = assertThrows(IllegalStateException.class, () ->
                SecurityConfig.resolveAllowedOrigins(new String[]{"http://localhost:5173", "http://127.0.0.1:3000"}, true)
        );
        assertTrue(exLocal.getMessage().contains("localhost origins are permitted for local development only"));

        // 3. Wildcard origin is strictly prohibited in any profile with credentials
        IllegalStateException exWildcardProd = assertThrows(IllegalStateException.class, () ->
                SecurityConfig.resolveAllowedOrigins(new String[]{"*"}, true)
        );
        assertTrue(exWildcardProd.getMessage().contains("Wildcard CORS origin '*' is strictly prohibited"));

        IllegalStateException exWildcardDev = assertThrows(IllegalStateException.class, () ->
                SecurityConfig.resolveAllowedOrigins(new String[]{"*"}, false)
        );
        assertTrue(exWildcardDev.getMessage().contains("Wildcard CORS origin '*' is strictly prohibited"));

        // 4. Valid production deployed frontend origin succeeds
        String prodFrontend = "https://borrowhub-frontend.dockhosting.dev";
        List<String> resolvedProd = SecurityConfig.resolveAllowedOrigins(new String[]{prodFrontend}, true);
        assertEquals(List.of(prodFrontend), resolvedProd);

        // 5. Development profile with no configuration safely defaults to local dev origins
        List<String> resolvedDev = SecurityConfig.resolveAllowedOrigins(new String[0], false);
        assertEquals(SecurityConfig.LOCAL_DEV_ORIGINS, resolvedDev);
    }
}
