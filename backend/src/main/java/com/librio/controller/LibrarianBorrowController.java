package com.librio.controller;

import com.librio.domain.Account;
import com.librio.dto.BorrowItemReferenceDto;
import com.librio.dto.BorrowRequestDto;
import com.librio.dto.BorrowingDto;
import com.librio.dto.RejectBorrowRequestDto;
import com.librio.security.CurrentAccountService;
import com.librio.service.BorrowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/librarian/borrow-requests")
@RequiredArgsConstructor
public class LibrarianBorrowController {

    private final BorrowService borrowService;
    private final CurrentAccountService currentAccountService;

    @GetMapping
    public ResponseEntity<List<BorrowRequestDto>> list() {
        Account librarian = currentAccountService.getCurrentAccount();
        return ResponseEntity.ok(borrowService.getAllRequests(librarian.getId()));
    }

    @PostMapping("/{id}/prepare")
    public ResponseEntity<BorrowRequestDto> prepare(
            @PathVariable Long id,
            @RequestBody(required = false) BorrowItemReferenceDto body) {
        Account librarian = currentAccountService.getCurrentAccount();
        Long physicalItemId = body == null ? null : body.getPhysicalItemId();
        return ResponseEntity.ok(borrowService.prepare(librarian.getId(), id, physicalItemId));
    }

    @PostMapping("/{id}/fulfil")
    public ResponseEntity<BorrowingDto> fulfil(
            @PathVariable Long id,
            @RequestBody(required = false) BorrowItemReferenceDto body) {
        Account librarian = currentAccountService.getCurrentAccount();
        Long physicalItemId = body == null ? null : body.getPhysicalItemId();
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .body(borrowService.fulfil(librarian.getId(), id, physicalItemId));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<BorrowRequestDto> reject(
            @PathVariable Long id,
            @RequestBody RejectBorrowRequestDto body) {
        Account librarian = currentAccountService.getCurrentAccount();
        return ResponseEntity.ok(borrowService.reject(librarian.getId(), id, body.getReason()));
    }

    @PostMapping("/{id}/expire")
    public ResponseEntity<BorrowRequestDto> expire(@PathVariable Long id) {
        Account librarian = currentAccountService.getCurrentAccount();
        return ResponseEntity.ok(borrowService.expire(librarian.getId(), id));
    }
}
