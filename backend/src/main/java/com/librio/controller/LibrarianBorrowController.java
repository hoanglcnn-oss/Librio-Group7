package com.librio.controller;

import com.librio.domain.Account;
import com.librio.dto.BorrowItemReferenceDto;
import com.librio.dto.LibrarianBorrowRequestItemDto;
import com.librio.dto.LibrarianBorrowRequestsResponseDto;
import com.librio.dto.LibrarianBorrowingDto;
import com.librio.dto.RejectBorrowRequestDto;
import com.librio.security.CurrentAccountService;
import com.librio.service.BorrowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/librarian/borrow-requests")
@RequiredArgsConstructor
public class LibrarianBorrowController {

    private final BorrowService borrowService;
    private final CurrentAccountService currentAccountService;

    @GetMapping
    public ResponseEntity<LibrarianBorrowRequestsResponseDto> list() {
        Account librarian = currentAccountService.getCurrentAccount();
        return ResponseEntity.ok(borrowService.getAllRequests(librarian.getId()));
    }

    @PostMapping("/{id}/prepare")
    public ResponseEntity<LibrarianBorrowRequestItemDto> prepare(
            @PathVariable Long id,
            @Valid @RequestBody BorrowItemReferenceDto body) {
        Account librarian = currentAccountService.getCurrentAccount();
        return ResponseEntity.ok(borrowService.prepare(librarian.getId(), id, body.getPhysicalItemId()));
    }

    @PostMapping("/{id}/fulfil")
    public ResponseEntity<LibrarianBorrowingDto> fulfil(
            @PathVariable Long id,
            @Valid @RequestBody BorrowItemReferenceDto body) {
        Account librarian = currentAccountService.getCurrentAccount();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(borrowService.fulfil(librarian.getId(), id, body.getPhysicalItemId()));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<LibrarianBorrowRequestItemDto> reject(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) RejectBorrowRequestDto body) {
        Account librarian = currentAccountService.getCurrentAccount();
        return ResponseEntity.ok(borrowService.reject(
                librarian.getId(), id, body == null ? null : body.getReason()));
    }
}
