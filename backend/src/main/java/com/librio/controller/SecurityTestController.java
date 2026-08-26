package com.librio.controller;

import com.librio.domain.Account;
import com.librio.security.CurrentAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me")
@RequiredArgsConstructor
public class SecurityTestController {

    private final CurrentAccountService currentAccountService;

    @GetMapping("/test")
    public String testReader() {

        Account account = currentAccountService.getCurrentAccount();

        return "Current reader: " + account.getEmail();
    }
}