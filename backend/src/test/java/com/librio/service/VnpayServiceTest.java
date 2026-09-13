package com.librio.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VnpayServiceTest {

    private VnpayService vnpayService;

    @BeforeEach
    void setUp() {
        vnpayService = new VnpayService();
        ReflectionTestUtils.setField(vnpayService, "vnpayPayUrl", "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
        ReflectionTestUtils.setField(vnpayService, "vnpayTmnCode", "TESTCODE");
        ReflectionTestUtils.setField(vnpayService, "vnpayHashSecret", "TESTSECRET");
        ReflectionTestUtils.setField(vnpayService, "vnpayReturnUrl", "http://localhost/return");
    }

    @Test
    void testBuildPaymentUrl() {
        String url = vnpayService.buildPaymentUrl("TXN123", 100000L, "Test Order", "127.0.0.1");
        assertNotNull(url);
        assertTrue(url.startsWith("https://sandbox.vnpayment.vn"));
        assertTrue(url.contains("vnp_TxnRef=TXN123"));
        assertTrue(url.contains("vnp_Amount=10000000")); // Amount * 100
        assertTrue(url.contains("vnp_SecureHash="));
    }

    @Test
    void testVerifyChecksumInvalid() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", "TXN123");
        params.put("vnp_Amount", "10000000");
        params.put("vnp_SecureHash", "invalidhash");
        
        assertFalse(vnpayService.verifyChecksum(params));
    }
}