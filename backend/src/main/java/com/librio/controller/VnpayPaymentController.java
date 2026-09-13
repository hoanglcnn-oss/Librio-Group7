package com.librio.controller;

import com.librio.dto.VnpayPaymentRequestDto;
import com.librio.dto.VnpayPaymentResponseDto;
import com.librio.service.MembershipService;
import com.librio.service.VnpayService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
public class VnpayPaymentController {

    private final MembershipService membershipService;
    private final VnpayService vnpayService;

    @PostMapping("/me/membership-payments/vnpay")
    public ResponseEntity<VnpayPaymentResponseDto> createPayment(
            Principal principal,
            @Valid @RequestBody VnpayPaymentRequestDto request,
            HttpServletRequest httpRequest
    ) {
        String ipAddr = httpRequest.getRemoteAddr();
        String paymentUrl = membershipService.createVnpayPayment(principal.getName(), request.getPlanId(), vnpayService, ipAddr);
        return ResponseEntity.ok(new VnpayPaymentResponseDto(paymentUrl));
    }
}