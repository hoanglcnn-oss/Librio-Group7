package com.librio.controller;

import com.librio.domain.Account;
import com.librio.dto.ReaderBorrowRequestItemDto;
import com.librio.dto.ReaderBorrowRequestsResponseDto;
import com.librio.security.CurrentAccountService;
import com.librio.service.BorrowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/me/borrow-requests")
@RequiredArgsConstructor
public class ReaderBorrowController {

    private final BorrowService borrowService;
    private final CurrentAccountService currentAccountService;

    @GetMapping
    public ResponseEntity<ReaderBorrowRequestsResponseDto> list() {
        Account reader = currentAccountService.getCurrentAccount();
        return ResponseEntity.ok(borrowService.getReaderRequests(reader.getId()));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ReaderBorrowRequestItemDto> cancel(@PathVariable Long id) {
        Account reader = currentAccountService.getCurrentAccount();
        return ResponseEntity.ok(borrowService.cancel(reader.getId(), id));
    }
}
