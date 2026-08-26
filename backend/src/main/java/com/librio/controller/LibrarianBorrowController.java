package com.librio.controller;

import com.librio.domain.Account;
import com.librio.dto.BorrowRequestDto;
import com.librio.dto.BorrowingDto;
import com.librio.security.CurrentAccountService;
import com.librio.service.BorrowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/librarian/borrow-requests")
@RequiredArgsConstructor
public class LibrarianBorrowController {

    private final BorrowService borrowService;
    private final CurrentAccountService currentAccountService;

    @PostMapping("/{id}/prepare")
    public ResponseEntity<BorrowRequestDto> prepare(@PathVariable Long id) {
        Account librarian = currentAccountService.getCurrentAccount();
        return ResponseEntity.ok(borrowService.prepare(librarian.getId(), id));
    }

    @PostMapping("/{id}/fulfil")
    public ResponseEntity<BorrowingDto> fulfil(@PathVariable Long id) {
        Account librarian = currentAccountService.getCurrentAccount();
        return ResponseEntity.ok(borrowService.fulfil(librarian.getId(), id));
    }
}
