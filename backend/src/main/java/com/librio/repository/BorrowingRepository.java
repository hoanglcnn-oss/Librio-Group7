package com.librio.repository;

import com.librio.domain.Borrowing;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

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

    @Query("""
            select b from Borrowing b
            join fetch b.borrowRequest br
            join fetch b.physicalItem pi
            join fetch pi.resource
            where b.reader.id = :readerId
              and b.returnedAt is null
            order by b.dueAt asc, b.borrowedAt asc, b.id asc
            """)
    List<Borrowing> findActiveByReaderId(@Param("readerId") Long readerId);

    @Query("""
            select b from Borrowing b
            join fetch b.reader
            join fetch b.borrowRequest
            join fetch b.physicalItem pi
            join fetch pi.resource
            where b.returnedAt is null
            order by b.dueAt asc, b.borrowedAt asc, b.id asc
            """)
    List<Borrowing> findActiveForLibrarian();

    /**
     * Return flow lock borrowing trước khi lock đúng physical item.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select b from Borrowing b
            join fetch b.reader
            join fetch b.borrowRequest
            join fetch b.physicalItem pi
            join fetch pi.resource
            where b.id = :id
            """)
    Optional<Borrowing> findByIdForUpdate(@Param("id") Long id);
}
