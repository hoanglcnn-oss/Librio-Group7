package com.librio.repository;

import com.librio.domain.MembershipSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MembershipSubscriptionRepository extends JpaRepository<MembershipSubscription, Long> {
    List<MembershipSubscription> findByAccountId(Long accountId);

    Optional<MembershipSubscription> findFirstByAccountIdOrderByStartsAtDescIdDesc(Long accountId);

    @Query("""
            select s from MembershipSubscription s
            join fetch s.plan
            where s.account.id = :accountId
              and s.startsAt <= :now
              and s.expiresAt > :now
            """)
    Optional<MembershipSubscription> findActiveByAccountId(
            @Param("accountId") Long accountId,
            @Param("now") LocalDateTime now
    );
}
