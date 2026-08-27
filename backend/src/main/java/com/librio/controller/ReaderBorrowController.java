package com.librio.controller;

import com.librio.domain.Account;
import com.librio.dto.BorrowRequestDto;
import com.librio.dto.CreateBorrowRequestDto;
import com.librio.security.CurrentAccountService;
import com.librio.service.BorrowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/me/borrow-requests")
@RequiredArgsConstructor
public class ReaderBorrowController {

    private final BorrowService borrowService;
    private final CurrentAccountService currentAccountService;

    @GetMapping
    public ResponseEntity<List<BorrowRequestDto>> list() {
        Account reader = currentAccountService.getCurrentAccount();
        return ResponseEntity.ok(borrowService.getReaderRequests(reader.getId()));
    }

    @PostMapping
    public ResponseEntity<BorrowRequestDto> create(@RequestBody CreateBorrowRequestDto body) {
        Account reader = currentAccountService.getCurrentAccount();
        BorrowRequestDto response = borrowService.createRequest(reader.getId(), body.getResourceId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<BorrowRequestDto> cancel(@PathVariable Long id) {
        Account reader = currentAccountService.getCurrentAccount();
        return ResponseEntity.ok(borrowService.cancel(reader.getId(), id));
    }
}
