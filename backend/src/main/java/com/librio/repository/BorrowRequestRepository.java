package com.librio.repository;

import com.librio.domain.BorrowRequest;
import com.librio.domain.BorrowRequestStatus;
import org.springframework.data.domain.Pageable;
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

    long countByReaderIdAndStatusIn(
            Long readerId,
            Collection<BorrowRequestStatus> statuses
    );

    Optional<BorrowRequest> findByIdAndReaderId(Long id, Long readerId);

    List<BorrowRequest> findByReaderIdOrderByRequestedAtDesc(Long readerId);

    List<BorrowRequest> findAllByOrderByRequestedAtDesc();

    /**
     * Lock request cùng exact item để chỉ một transition lifecycle được commit.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select br from BorrowRequest br " +
            "join fetch br.reader " +
            "join fetch br.resource " +
            "join fetch br.physicalItem pi " +
            "join fetch pi.resource " +
            "where br.id = :id")
    Optional<BorrowRequest> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select br from BorrowRequest br " +
            "join fetch br.reader " +
            "join fetch br.resource " +
            "join fetch br.physicalItem pi " +
            "join fetch pi.resource " +
            "where br.id = :id and br.reader.id = :readerId")
    Optional<BorrowRequest> findByIdAndReaderIdForUpdate(@Param("id") Long id, @Param("readerId") Long readerId);

    @Query("""
            select br from BorrowRequest br
            join fetch br.resource
            join fetch br.physicalItem pi
            where br.reader.id = :readerId
              and br.status in :statuses
            order by case br.status
                       when com.librio.domain.BorrowRequestStatus.READY_FOR_PICKUP then 0
                       when com.librio.domain.BorrowRequestStatus.REQUESTED then 1
                       else 2
                     end,
                     case when br.expiresAt is null then 1 else 0 end,
                     br.expiresAt asc,
                     br.requestedAt asc,
                     br.id asc
            """)
    List<BorrowRequest> findActiveForReader(@Param("readerId") Long readerId,
                                            @Param("statuses") Collection<BorrowRequestStatus> statuses);

    @Query("""
            select br from BorrowRequest br
            join fetch br.resource
            join fetch br.physicalItem pi
            where br.reader.id = :readerId
              and br.status in :statuses
            order by br.statusUpdatedAt desc, br.id asc
            """)
    List<BorrowRequest> findRecentOutcomesForReader(@Param("readerId") Long readerId,
                                                    @Param("statuses") Collection<BorrowRequestStatus> statuses,
                                                    Pageable pageable);

    @Query("""
            select br from BorrowRequest br
            join fetch br.reader
            join fetch br.resource
            join fetch br.physicalItem pi
            where br.status in :statuses
            order by br.requestedAt asc, br.id asc
            """)
    List<BorrowRequest> findActiveForLibrarian(@Param("statuses") Collection<BorrowRequestStatus> statuses);

    @Query("""
            select br from BorrowRequest br
            join fetch br.reader
            join fetch br.resource
            join fetch br.physicalItem pi
            where br.status in :statuses
            order by br.statusUpdatedAt desc, br.id desc
            """)
    List<BorrowRequest> findRecentOutcomesForLibrarian(
            @Param("statuses") Collection<BorrowRequestStatus> statuses,
            Pageable pageable);

    /**
     * Scheduler lock active request hết hạn để expire và release reservation trong cùng transaction.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select br from BorrowRequest br
            join fetch br.physicalItem pi
            where br.status in :statuses
              and br.expiresAt is not null
              and br.expiresAt <= :now
            order by br.expiresAt asc, br.id asc
            """)
    List<BorrowRequest> findExpiredActiveForUpdate(@Param("now") java.time.LocalDateTime now,
                                                   @Param("statuses") Collection<BorrowRequestStatus> statuses);
}
