package com.librio.config;

import com.librio.domain.Account;
import com.librio.domain.AccountRole;
import com.librio.domain.AccountStatus;
import com.librio.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Profile("!prod")
@RequiredArgsConstructor
public class DevAccountSeeder implements CommandLineRunner {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {

        String readerPassword = System.getenv("LIBRIO_SEED_READER_PASSWORD");
        String librarianPassword = System.getenv("LIBRIO_SEED_LIBRARIAN_PASSWORD");

        if (readerPassword == null || librarianPassword == null) {
            return;
        }

        createAccountIfMissing(
                "reader@librio.local",
                "Reader",
                AccountRole.READER,
                readerPassword
        );

        createAccountIfMissing(
                "librarian@librio.local",
                "Librarian",
                AccountRole.LIBRARIAN,
                librarianPassword
        );
    }

    private void createAccountIfMissing(
            String email,
            String displayName,
            AccountRole role,
            String rawPassword
    ) {
        String normalizedEmail = email.trim().toLowerCase();

        if (accountRepository.findByEmail(normalizedEmail).isPresent()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        Account account = Account.builder()
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .displayName(displayName)
                .role(role)
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();

        accountRepository.save(account);
    }
}