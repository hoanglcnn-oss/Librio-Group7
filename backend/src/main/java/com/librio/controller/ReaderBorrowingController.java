package com.librio.controller;

import com.librio.domain.Account;
import com.librio.dto.ReaderBorrowingsResponseDto;
import com.librio.security.CurrentAccountService;
import com.librio.service.BorrowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me/borrowings")
@RequiredArgsConstructor
public class ReaderBorrowingController {

    private final BorrowService borrowService;
    private final CurrentAccountService currentAccountService;

    @GetMapping
    public ResponseEntity<ReaderBorrowingsResponseDto> list() {
        Account reader = currentAccountService.getCurrentAccount();
        return ResponseEntity.ok(borrowService.getReaderBorrowings(reader.getId()));
    }
}
