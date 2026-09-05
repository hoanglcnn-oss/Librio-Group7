package com.librio.repository;

import com.librio.domain.PhysicalItem;
import com.librio.domain.PhysicalItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

@Repository
public interface PhysicalItemRepository extends JpaRepository<PhysicalItem, Long> {

    List<PhysicalItem> findByResourceId(Long resourceId);

    long countByResourceId(Long resourceId);

    long countByResourceIdAndStatus(Long resourceId, PhysicalItemStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PhysicalItem p where p.id = :id")
    Optional<PhysicalItem> findByIdForUpdate(@Param("id") Long id);

    /**
     * Lock các copy ứng viên trước khi reserve/reconcile để các flow cạnh tranh không claim cùng item.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PhysicalItem p where p.resource.id = :resourceId " +
            "and p.status = :status order by p.id asc")
    List<PhysicalItem> findForUpdate(
            @Param("resourceId") Long resourceId,
            @Param("status") PhysicalItemStatus status,
            Pageable pageable
    );
}
