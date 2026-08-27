package com.librio.controller;

import com.librio.domain.Account;
import com.librio.dto.AccountSummaryDto;
import com.librio.dto.LoginRequest;
import com.librio.exception.BorrowErrorCode;
import com.librio.exception.BorrowFlowException;
import com.librio.repository.AccountRepository;
import com.librio.security.CurrentAccountService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
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
    private final AccountRepository accountRepository;
    private final CurrentAccountService currentAccountService;

    @PostMapping("/login")
    public AccountSummaryDto login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {

        String email = request.getEmail().trim().toLowerCase();
        Authentication authentication;
        try {
            authentication =
                    authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                email,
                                request.getPassword()
                        )
                );
        } catch (AuthenticationException ex) {
            throw new BorrowFlowException(BorrowErrorCode.INVALID_CREDENTIALS.name(), HttpStatus.UNAUTHORIZED,
                    "Invalid credentials");
        }

        SecurityContext securityContext =
                SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        HttpSession session = httpRequest.getSession(true);

        session.setAttribute(
                "SPRING_SECURITY_CONTEXT",
                securityContext
        );

        return toSummary(findAccount(authentication.getName()));
    }

    @GetMapping("/me")
    public AccountSummaryDto me(Authentication authentication) {
        return toSummary(currentAccountService.getCurrentAccount());
    }

    private Account findAccount(String email) {
        return accountRepository.findByEmail(email)
                .orElseThrow(() -> new BorrowFlowException(BorrowErrorCode.AUTHENTICATION_REQUIRED.name(),
                        HttpStatus.UNAUTHORIZED, "Authentication required"));
    }

    private AccountSummaryDto toSummary(Account account) {
        return AccountSummaryDto.builder()
                .id(account.getId())
                .email(account.getEmail())
                .displayName(account.getDisplayName())
                .role(account.getRole())
                .accountStatus(account.getAccountStatus())
                .build();
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of(
                "token", token.getToken(),
                "headerName", token.getHeaderName(),
                "parameterName", token.getParameterName()
        );
    }

}
