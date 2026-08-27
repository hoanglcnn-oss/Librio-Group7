package com.librio.repository;

import com.librio.domain.BorrowRequest;
import com.librio.domain.BorrowRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BorrowRequestRepository extends JpaRepository<BorrowRequest, Long> {

    boolean existsByReaderIdAndResourceIdAndStatusIn(
            Long readerId,
            Long resourceId,
            Collection<BorrowRequestStatus> statuses
    );

    Optional<BorrowRequest> findByIdAndReaderId(Long id, Long readerId);

    List<BorrowRequest> findByReaderIdOrderByRequestedAtDesc(Long readerId);

    List<BorrowRequest> findAllByOrderByRequestedAtDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select br from BorrowRequest br " +
            "join fetch br.physicalItem where br.id = :id")
    Optional<BorrowRequest> findByIdForUpdate(@Param("id") Long id);
}
