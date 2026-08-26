package com.librio.controller;

import com.librio.dto.LoginRequest;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.web.csrf.CsrfToken;
import jakarta.servlet.http.HttpSession;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;

    @PostMapping("/login")
    public Map<String, Object> login(
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {

        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                request.getEmail(),
                                request.getPassword()
                        )
                );

        SecurityContext securityContext =
                SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        HttpSession session = httpRequest.getSession(true);

        session.setAttribute(
                "SPRING_SECURITY_CONTEXT",
                securityContext
        );

        return Map.of(
                "email", authentication.getName(),
                "roles", authentication.getAuthorities()
        );
    }
    @GetMapping("/me")
    public Map<String, Object> me(Authentication authentication) {

        return Map.of(
                "email", authentication.getName(),
                "roles", authentication.getAuthorities()
        );
    }
    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of(
                "token", token.getToken(),
                "headerName", token.getHeaderName()
        );
    }

}
