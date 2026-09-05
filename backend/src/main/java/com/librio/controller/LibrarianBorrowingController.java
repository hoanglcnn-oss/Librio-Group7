package com.librio.controller;

import com.librio.domain.Account;
import com.librio.dto.LibrarianBorrowingDto;
import com.librio.dto.LibrarianBorrowingsResponseDto;
import com.librio.exception.BorrowErrorCode;
import com.librio.exception.BorrowFlowException;
import com.librio.security.CurrentAccountService;
import com.librio.service.BorrowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/librarian/borrowings")
@RequiredArgsConstructor
public class LibrarianBorrowingController {
    private final BorrowService borrowService;
    private final CurrentAccountService currentAccountService;

    @GetMapping
    public ResponseEntity<LibrarianBorrowingsResponseDto> list(
            @RequestParam(defaultValue = "active") String status) {
        if (!"active".equalsIgnoreCase(status)) {
            throw new BorrowFlowException(BorrowErrorCode.VALIDATION_ERROR.name(), HttpStatus.BAD_REQUEST,
                    "Only status=active is supported");
        }
        Account librarian = currentAccountService.getCurrentAccount();
        return ResponseEntity.ok(borrowService.getActiveBorrowingsForLibrarian(librarian.getId()));
    }

    @PostMapping("/{id}/return")
    public ResponseEntity<LibrarianBorrowingDto> returnBorrowing(@PathVariable Long id) {
        Account librarian = currentAccountService.getCurrentAccount();
        return ResponseEntity.ok(borrowService.returnBorrowing(librarian.getId(), id));
    }
}
