package com.librio.config;

import com.librio.security.AccountUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.AuthenticationEntryPoint;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final AccountUserDetailsService userDetailsService;

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
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.setStatus(HttpServletResponse.SC_NO_CONTENT)
                        )
                )

                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) ->
                                response.sendError(
                                        HttpServletResponse.SC_UNAUTHORIZED
                                )
                        )
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                response.sendError(
                                        HttpServletResponse.SC_FORBIDDEN
                                )
                        )
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/health").permitAll()
                        .requestMatchers("/resources/**").permitAll()
                        .requestMatchers("/auth/login").permitAll()
                        .requestMatchers("/auth/csrf").permitAll()
                        .requestMatchers("/auth/me").authenticated()
                        .requestMatchers("/me/**").hasRole("READER")
                        .requestMatchers("/librarian/**").hasRole("LIBRARIAN")
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}
