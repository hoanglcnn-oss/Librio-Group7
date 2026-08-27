package com.librio.repository;

import com.librio.domain.Borrowing;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface BorrowingRepository extends JpaRepository<Borrowing, Long> {
    boolean existsByBorrowRequestId(Long borrowRequestId);

    @Query("""
            select case when count(b) > 0 then true else false end
            from Borrowing b
            where b.reader.id = :readerId
              and b.physicalItem.resource.id = :resourceId
              and b.returnedAt is null
            """)
    boolean existsActiveBorrowingByReaderIdAndResourceId(
            @Param("readerId") Long readerId,
            @Param("resourceId") Long resourceId
    );

    @Query("""
            select count(b)
            from Borrowing b
            where b.reader.id = :readerId
              and b.returnedAt is null
            """)
    long countActiveBorrowingsByReaderId(@Param("readerId") Long readerId);
}
