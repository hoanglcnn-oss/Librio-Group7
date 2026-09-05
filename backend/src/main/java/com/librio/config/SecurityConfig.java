package com.librio.config;

import com.librio.security.AccountUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.librio.dto.ErrorResponseDto;
import com.librio.exception.BorrowErrorCode;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.OffsetDateTime;
import org.springframework.http.HttpMethod;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final AccountUserDetailsService userDetailsService;
    private final ObjectMapper objectMapper;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider();

        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());

        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
                .authenticationProvider(authenticationProvider())

                .csrf(csrf -> csrf
                        .csrfTokenRepository(
                                CookieCsrfTokenRepository.withHttpOnlyFalse()
                        )
                )

                .logout(logout -> logout
                        .logoutUrl("/auth/logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler((request, response, authentication) -> {
                            if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
                                writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                                        BorrowErrorCode.AUTHENTICATION_REQUIRED.name(),
                                        "Authentication required");
                                return;
                            }
                            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                        })
                )

                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) ->
                                writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                                        BorrowErrorCode.AUTHENTICATION_REQUIRED.name(),
                                        "Authentication required"))
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            if (accessDeniedException instanceof CsrfException) {
                                writeError(response, HttpServletResponse.SC_FORBIDDEN,
                                        BorrowErrorCode.CSRF_TOKEN_INVALID.name(),
                                        "CSRF token is missing or invalid");
                                return;
                            }
                            writeError(response, HttpServletResponse.SC_FORBIDDEN,
                                    BorrowErrorCode.OPERATION_FORBIDDEN.name(),
                                    "Operation forbidden");
                        })
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/health").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/resources/*/digital-access",
                                "/resources/*/digital-content").hasRole("READER")
                        .requestMatchers("/resources/**").permitAll()
                        .requestMatchers("/auth/login").permitAll()
                        .requestMatchers("/auth/csrf").permitAll()
                        .requestMatchers("/auth/me").authenticated()
                        .requestMatchers(HttpMethod.POST, "/borrow-requests").hasRole("READER")
                        .requestMatchers("/me/**").hasRole("READER")
                        .requestMatchers("/librarian/**").hasRole("LIBRARIAN")
                        .anyRequest().authenticated()
                );

        return http.build();
    }

    private void writeError(HttpServletResponse response, int status, String code, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        objectMapper.writeValue(response.getWriter(), new ErrorResponseDto(status, code, message, OffsetDateTime.now()));
    }
}
