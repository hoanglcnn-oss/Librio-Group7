package com.librio.repository;

import com.librio.domain.MembershipSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MembershipSubscriptionRepository extends JpaRepository<MembershipSubscription, Long> {
    List<MembershipSubscription> findByAccountId(Long accountId);
}
