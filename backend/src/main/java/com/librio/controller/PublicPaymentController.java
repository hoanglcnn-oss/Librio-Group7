package com.librio.controller;

import com.librio.service.MembershipService;
import com.librio.service.VnpayService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class PublicPaymentController {

    private final MembershipService membershipService;
    private final VnpayService vnpayService;

    @Value("${librio.frontendUrl}")
    private String frontendUrl;

    @GetMapping("/payments/vnpay-return")
    public void vnpayReturn(@RequestParam Map<String, String> params, HttpServletResponse response) throws IOException {
        if (!vnpayService.verifyChecksum(params)) {
            response.sendRedirect(frontendUrl + "/membership?payment=invalid");
            return;
        }

        boolean success = membershipService.processVnpayReturn(params);
        if (success) {
            response.sendRedirect(frontendUrl + "/membership?payment=success");
        } else {
            response.sendRedirect(frontendUrl + "/membership?payment=failed");
        }
    }
}