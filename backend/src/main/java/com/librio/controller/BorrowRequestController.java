package com.librio.controller;

import com.librio.domain.Account;
import com.librio.dto.BorrowRequestDto;
import com.librio.dto.CreateBorrowRequestDto;
import com.librio.security.CurrentAccountService;
import com.librio.service.BorrowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class BorrowRequestController {

    private final BorrowService borrowService;
    private final CurrentAccountService currentAccountService;

    @PostMapping("/borrow-requests")
    public ResponseEntity<BorrowRequestDto> create(@RequestBody CreateBorrowRequestDto body) {
        Account reader = currentAccountService.getCurrentAccount();
        BorrowRequestDto response = borrowService.createRequest(reader.getId(), body.getResourceId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
