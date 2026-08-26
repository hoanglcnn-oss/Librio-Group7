package com.librio.repository;

import com.librio.domain.BorrowRequest;
import com.librio.domain.BorrowRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface BorrowRequestRepository extends JpaRepository<BorrowRequest, Long> {

    boolean existsByReaderIdAndResourceIdAndStatusIn(
            Long readerId,
            Long resourceId,
            Collection<BorrowRequestStatus> statuses
    );

    Optional<BorrowRequest> findByIdAndReaderId(Long id, Long readerId);
}
