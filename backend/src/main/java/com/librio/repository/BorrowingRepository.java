package com.librio.repository;

import com.librio.domain.Borrowing;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BorrowingRepository extends JpaRepository<Borrowing, Long> {
    boolean existsByBorrowRequestId(Long borrowRequestId);
}
