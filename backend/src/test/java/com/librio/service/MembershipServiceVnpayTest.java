package com.librio.service;

import com.librio.domain.Account;
import com.librio.domain.MembershipPlan;
import com.librio.domain.MembershipSubscription;
import com.librio.domain.PaymentStatus;
import com.librio.domain.PaymentTransaction;
import com.librio.repository.AccountRepository;
import com.librio.repository.MembershipPlanRepository;
import com.librio.repository.MembershipSubscriptionRepository;
import com.librio.repository.PaymentTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MembershipServiceVnpayTest {

    @Mock
    private MembershipPlanRepository membershipPlanRepository;
    @Mock
    private MembershipSubscriptionRepository membershipSubscriptionRepository;
    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;
    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private MembershipService membershipService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void processVnpayReturn_Success_CreatesMembership() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", "TXN123");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TransactionNo", "VN12345");

        Account account = Account.builder().id(1L).build();
        MembershipPlan plan = MembershipPlan.builder().id(1L).durationMonths(1).build();
        PaymentTransaction tx = PaymentTransaction.builder()
                .id(1L)
                .account(account)
                .plan(plan)
                .status(PaymentStatus.PENDING)
                .providerTxnRef("TXN123")
                .build();

        when(paymentTransactionRepository.findByProviderTxnRef("TXN123")).thenReturn(Optional.of(tx));
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(account));
        when(membershipSubscriptionRepository.findFirstByAccountIdOrderByStartsAtDescIdDesc(1L)).thenReturn(Optional.empty());

        boolean result = membershipService.processVnpayReturn(params);

        assertTrue(result);
        assertEquals(PaymentStatus.SUCCESS, tx.getStatus());
        assertEquals("00", tx.getProviderResponseCode());
        assertEquals("VN12345", tx.getProviderTransactionNo());
        assertNotNull(tx.getCompletedAt());

        verify(membershipSubscriptionRepository, times(1)).save(any(MembershipSubscription.class));
    }

    @Test
    void processVnpayReturn_Failed_DoesNotCreateMembership() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", "TXN123");
        params.put("vnp_ResponseCode", "24");
        params.put("vnp_TransactionNo", "VN12345");

        Account account = Account.builder().id(1L).build();
        MembershipPlan plan = MembershipPlan.builder().id(1L).durationMonths(1).build();
        PaymentTransaction tx = PaymentTransaction.builder()
                .id(1L)
                .account(account)
                .plan(plan)
                .status(PaymentStatus.PENDING)
                .providerTxnRef("TXN123")
                .build();

        when(paymentTransactionRepository.findByProviderTxnRef("TXN123")).thenReturn(Optional.of(tx));

        boolean result = membershipService.processVnpayReturn(params);

        assertFalse(result);
        assertEquals(PaymentStatus.FAILED, tx.getStatus());
        assertEquals("24", tx.getProviderResponseCode());
        assertEquals("VN12345", tx.getProviderTransactionNo());
        assertNotNull(tx.getCompletedAt());

        verify(membershipSubscriptionRepository, never()).save(any(MembershipSubscription.class));
    }

    @Test
    void processVnpayReturn_Idempotent_RepeatedSuccess() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", "TXN123");
        params.put("vnp_ResponseCode", "00");

        PaymentTransaction tx = PaymentTransaction.builder()
                .id(1L)
                .status(PaymentStatus.SUCCESS)
                .providerTxnRef("TXN123")
                .build();

        when(paymentTransactionRepository.findByProviderTxnRef("TXN123")).thenReturn(Optional.of(tx));

        boolean result = membershipService.processVnpayReturn(params);

        assertTrue(result);
        verify(membershipSubscriptionRepository, never()).save(any(MembershipSubscription.class));
    }
}