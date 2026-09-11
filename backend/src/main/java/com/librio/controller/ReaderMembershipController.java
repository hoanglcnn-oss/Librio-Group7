package com.librio.controller;

import com.librio.dto.MembershipPaymentRequestDto;
import com.librio.dto.ReaderMembershipDto;
import com.librio.service.MembershipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
public class ReaderMembershipController {

    private final MembershipService membershipService;

    @GetMapping("/me/membership")
    public ResponseEntity<ReaderMembershipDto> getCurrentMembership(Principal principal) {
        return ResponseEntity.ok(membershipService.getCurrentMembership(principal.getName()));
    }

    @PostMapping("/me/membership-payments")
    public ResponseEntity<ReaderMembershipDto> processPayment(
            Principal principal,
            @Valid @RequestBody MembershipPaymentRequestDto request
    ) {
        return ResponseEntity.ok(membershipService.processPayment(principal.getName(), request));
    }
}
