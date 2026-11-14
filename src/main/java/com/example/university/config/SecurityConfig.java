package com.example.university.config;

import com.example.university.dto.ApiError;
import com.example.university.repository.UserRepository;
import com.example.university.security.AccountJwtAuthenticationConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import java.io.IOException;
import java.util.Locale;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(UserRepository users) {
        return email -> users.findByEmailIgnoreCase(email.trim().toLowerCase(Locale.ROOT))
                .map(user -> User.withUsername(user.getEmail())
                        .password(user.getPassword()).roles(user.getRole().name()).build())
                .orElseThrow(() -> new UsernameNotFoundException("Invalid email or password"));
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper json,
            AccountJwtAuthenticationConverter accounts) throws Exception {
        http
            // API authentication uses only an explicit Bearer header, never cookies.
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .requestCache(cache -> cache.disable())
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .logout(logout -> logout.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/css/**", "/js/**", "/error").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .requestMatchers("/api/me", "/api/auth/logout").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/admins", "/api/admins/**",
                        "/api/teachers", "/api/teachers/**", "/api/students", "/api/students/**").hasRole("ADMIN")
                .requestMatchers("/api/admins", "/api/admins/**").hasRole("ADMIN")
                .requestMatchers("/api/teachers", "/api/teachers/**").hasAnyRole("ADMIN", "TEACHER")
                .requestMatchers("/api/students", "/api/students/**").hasAnyRole("ADMIN", "STUDENT")
                .anyRequest().denyAll())
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, exception) ->
                        writeError(response, json, 401, "A valid Bearer token is required"))
                .accessDeniedHandler((request, response, exception) ->
                        writeError(response, json, 403, "Access denied")))
            .oauth2ResourceServer(resource -> resource
                .jwt(jwt -> jwt.jwtAuthenticationConverter(accounts))
                .authenticationEntryPoint((request, response, exception) ->
                        writeError(response, json, 401, "Token is invalid, expired, or revoked"))
                .accessDeniedHandler((request, response, exception) ->
                        writeError(response, json, 403, "Access denied")));
        return http.build();
    }

    private static void writeError(HttpServletResponse response, ObjectMapper json,
                                   int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        if (status == 401) response.setHeader("WWW-Authenticate", "Bearer");
        json.writeValue(response.getWriter(), new ApiError(status, message));
    }
}
