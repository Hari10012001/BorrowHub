package com.borrowhub.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    public static final List<String> LOCAL_DEV_ORIGINS = List.of(
            "http://localhost:5173",
            "http://localhost:3000"
    );

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationEntryPoint unauthorizedHandler;
    private final JwtAccessDeniedHandler accessDeniedHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final List<String> allowedOrigins;

    public SecurityConfig(CustomUserDetailsService userDetailsService,
                          JwtAuthenticationEntryPoint unauthorizedHandler,
                          JwtAccessDeniedHandler accessDeniedHandler,
                          JwtAuthenticationFilter jwtAuthenticationFilter,
                          @Value("${borrowhub.cors.allowed-origins:}") String[] allowedOrigins,
                          Environment env) {
        this.userDetailsService = userDetailsService;
        this.unauthorizedHandler = unauthorizedHandler;
        this.accessDeniedHandler = accessDeniedHandler;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;

        boolean isProduction = env != null && Arrays.asList(env.getActiveProfiles()).contains("prod");
        this.allowedOrigins = resolveAllowedOrigins(allowedOrigins, isProduction);
        logger.info("SecurityConfig: CORS initialized with {} explicitly configured origin(s). allowCredentials=true",
                this.allowedOrigins.size());
    }

    /**
     * Resolves and validates allowed CORS origins based on the active runtime profile.
     * Enforces strict security invariants:
     * 1. Wildcard '*' is strictly prohibited when allowCredentials is true.
     * 2. Production profile requires explicit, non-empty CORS_ALLOWED_ORIGINS containing the deployed frontend origin.
     * 3. Localhost origins are permitted in development profile only.
     */
    public static List<String> resolveAllowedOrigins(String[] rawOrigins, boolean isProduction) {
        List<String> origins = rawOrigins != null ? Arrays.stream(rawOrigins)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList() : Collections.emptyList();

        if (origins.contains("*")) {
            throw new IllegalStateException("Wildcard CORS origin '*' is strictly prohibited when allowCredentials is true.");
        }

        if (isProduction) {
            if (origins.isEmpty()) {
                throw new IllegalStateException(
                        "In production profile ('prod'), CORS_ALLOWED_ORIGINS is required and must contain the exact deployed frontend origin."
                );
            }
            boolean hasOnlyLocalhost = origins.stream().allMatch(o ->
                    o.contains("localhost") || o.contains("127.0.0.1") || o.contains("0.0.0.0"));
            if (hasOnlyLocalhost) {
                throw new IllegalStateException(
                        "In production profile ('prod'), CORS_ALLOWED_ORIGINS must contain the deployed frontend origin (localhost origins are permitted for local development only)."
                );
            }
            return origins;
        }

        // Development / non-production profile: fallback to standard local frontend origins
        if (origins.isEmpty()) {
            return LOCAL_DEV_ORIGINS;
        }

        return origins;
    }

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(unauthorizedHandler)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/resources/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/uploads/**").permitAll()
                        .requestMatchers("/error").permitAll()

                        // Admin only endpoints
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/resources/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/resources/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/resources/**").hasRole("ADMIN")
                        .requestMatchers("/api/settings/**").hasRole("ADMIN")

                        // Circulation desk operations (ADMIN or FACULTY)
                        .requestMatchers("/api/circulation/checkout").hasAnyRole("ADMIN", "FACULTY")
                        .requestMatchers("/api/circulation/return").hasAnyRole("ADMIN", "FACULTY")
                        .requestMatchers("/api/circulation/damage-report").hasAnyRole("ADMIN", "FACULTY")
                        .requestMatchers("/api/circulation/loss-declaration").hasAnyRole("ADMIN", "FACULTY")

                        // Borrowing and Reservation requests
                        .requestMatchers("/api/borrowings/**").authenticated()
                        .requestMatchers("/api/reservations/**").authenticated()
                        .requestMatchers("/api/waitlist/**").authenticated()
                        .requestMatchers("/api/users/**").authenticated()

                        // Everything else requires authentication
                        .anyRequest().authenticated()
                );

        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Allow credentials ONLY with explicitly configured origins (never wildcard '*')
        configuration.setAllowedOrigins(this.allowedOrigins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Idempotency-Key", "X-Requested-With", "Accept"));
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Idempotency-Key"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
