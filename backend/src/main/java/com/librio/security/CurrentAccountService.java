package com.librio.security;

import com.librio.domain.Account;
import com.librio.exception.BorrowErrorCode;
import com.librio.exception.BorrowFlowException;
import com.librio.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentAccountService {

    private final AccountRepository accountRepository;

    public Account getCurrentAccount() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BorrowFlowException(BorrowErrorCode.AUTHENTICATION_REQUIRED.name(), HttpStatus.UNAUTHORIZED,
                    "Authentication required");
        }

        String email = authentication.getName();

        return accountRepository.findByEmail(email)
                .orElseThrow(() ->
                        new BorrowFlowException(BorrowErrorCode.AUTHENTICATION_REQUIRED.name(), HttpStatus.UNAUTHORIZED,
                                "Authentication required")
                );
    }
}
