package com.librio.repository;

import com.librio.domain.CirculationStatus;
import com.librio.domain.InventoryStatus;
import com.librio.domain.PhysicalItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

@Repository
public interface PhysicalItemRepository extends JpaRepository<PhysicalItem, Long> {

    List<PhysicalItem> findByResourceId(Long resourceId);

    boolean existsByBarcode(String barcode);

    boolean existsByBarcodeAndIdNot(String barcode, Long id);

    long countByResourceId(Long resourceId);

    long countByResourceIdAndInventoryStatusNot(Long resourceId, InventoryStatus status);

    long countByResourceIdAndInventoryStatusAndCirculationStatus(
            Long resourceId,
            InventoryStatus inventoryStatus,
            CirculationStatus circulationStatus
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PhysicalItem p where p.id = :id")
    Optional<PhysicalItem> findByIdForUpdate(@Param("id") Long id);

    /**
     * Lock các copy ứng viên trước khi reserve/reconcile để các flow cạnh tranh không claim cùng item.
     * Candidate selection yêu cầu: inventoryStatus = ACTIVE AND circulationStatus = AVAILABLE.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PhysicalItem p where p.resource.id = :resourceId " +
            "and p.inventoryStatus = :inventoryStatus " +
            "and p.circulationStatus = :circulationStatus " +
            "order by p.id asc")
    List<PhysicalItem> findForUpdate(
            @Param("resourceId") Long resourceId,
            @Param("inventoryStatus") InventoryStatus inventoryStatus,
            @Param("circulationStatus") CirculationStatus circulationStatus,
            Pageable pageable
    );

    @Query(
        value = """
                select p.*
                from physical_item p
                join resource r on r.id = p.resource_id
                where (:inventoryStatus = '' or p.inventory_status = :inventoryStatus)
                  and (:circulationStatus = '' or p.circulation_status = :circulationStatus)
                  and (
                        :q = ''
                        or lower(p.barcode) like lower(concat('%', :q, '%'))
                        or lower(p.location) like lower(concat('%', :q, '%'))
                        or lower(r.title) like lower(concat('%', :q, '%'))
                        or lower(r.authors) like lower(concat('%', :q, '%'))
                  )
                  and (
                        :attentionMode = -1
                        or (
                            :attentionMode = 1
                            and p.inventory_status <> 'WITHDRAWN'
                            and (
                                p.inventory_status in ('LOST', 'DAMAGED')
                                or p.location is null
                                or trim(p.location) = ''
                                or upper(trim(p.location)) = 'UNASSIGNED'
                                or exists (
                                    select 1 from borrowing b
                                    where b.physical_item_id = p.id
                                      and b.returned_at is null
                                      and b.due_at < :now
                                )
                                or (
                                    exists (
                                        select 1 from borrowing b
                                        where b.physical_item_id = p.id
                                          and b.returned_at is null
                                    )
                                    and exists (
                                        select 1 from borrow_request br
                                        where br.physical_item_id = p.id
                                          and br.status in ('REQUESTED', 'READY_FOR_PICKUP')
                                    )
                                )
                                or (
                                    p.circulation_status = 'RESERVED'
                                    and not exists (
                                        select 1 from borrow_request br
                                        where br.physical_item_id = p.id
                                          and br.status in ('REQUESTED', 'READY_FOR_PICKUP')
                                    )
                                )
                                or (
                                    p.circulation_status = 'BORROWED'
                                    and not exists (
                                        select 1 from borrowing b
                                        where b.physical_item_id = p.id
                                          and b.returned_at is null
                                    )
                                )
                                or (
                                    p.circulation_status = 'AVAILABLE'
                                    and (
                                        exists (
                                            select 1 from borrowing b
                                            where b.physical_item_id = p.id
                                              and b.returned_at is null
                                        )
                                        or exists (
                                            select 1 from borrow_request br
                                            where br.physical_item_id = p.id
                                              and br.status in ('REQUESTED', 'READY_FOR_PICKUP')
                                        )
                                    )
                                )
                            )
                        )
                        or (
                            :attentionMode = 0
                            and (
                                p.inventory_status = 'WITHDRAWN'
                                or not (
                                    p.inventory_status in ('LOST', 'DAMAGED')
                                    or p.location is null
                                    or trim(p.location) = ''
                                    or upper(trim(p.location)) = 'UNASSIGNED'
                                    or exists (
                                        select 1 from borrowing b
                                        where b.physical_item_id = p.id
                                          and b.returned_at is null
                                          and b.due_at < :now
                                    )
                                    or (
                                        exists (
                                            select 1 from borrowing b
                                            where b.physical_item_id = p.id
                                              and b.returned_at is null
                                        )
                                        and exists (
                                            select 1 from borrow_request br
                                            where br.physical_item_id = p.id
                                              and br.status in ('REQUESTED', 'READY_FOR_PICKUP')
                                        )
                                    )
                                    or (
                                        p.circulation_status = 'RESERVED'
                                        and not exists (
                                            select 1 from borrow_request br
                                            where br.physical_item_id = p.id
                                              and br.status in ('REQUESTED', 'READY_FOR_PICKUP')
                                        )
                                    )
                                    or (
                                        p.circulation_status = 'BORROWED'
                                        and not exists (
                                            select 1 from borrowing b
                                            where b.physical_item_id = p.id
                                              and b.returned_at is null
                                        )
                                    )
                                    or (
                                        p.circulation_status = 'AVAILABLE'
                                        and (
                                            exists (
                                                select 1 from borrowing b
                                                where b.physical_item_id = p.id
                                                  and b.returned_at is null
                                            )
                                            or exists (
                                                select 1 from borrow_request br
                                                where br.physical_item_id = p.id
                                                  and br.status in ('REQUESTED', 'READY_FOR_PICKUP')
                                            )
                                        )
                                    )
                                )
                            )
                        )
                  )
                order by
                    case when (
                        p.inventory_status <> 'WITHDRAWN'
                        and (
                            p.inventory_status in ('LOST', 'DAMAGED')
                            or p.location is null
                            or trim(p.location) = ''
                            or upper(trim(p.location)) = 'UNASSIGNED'
                            or exists (
                                select 1 from borrowing b
                                where b.physical_item_id = p.id
                                  and b.returned_at is null
                                  and b.due_at < :now
                            )
                            or (
                                p.circulation_status = 'RESERVED'
                                and not exists (
                                    select 1 from borrow_request br
                                    where br.physical_item_id = p.id
                                      and br.status in ('REQUESTED', 'READY_FOR_PICKUP')
                                )
                            )
                            or (
                                p.circulation_status = 'BORROWED'
                                and not exists (
                                    select 1 from borrowing b
                                    where b.physical_item_id = p.id
                                      and b.returned_at is null
                                )
                            )
                        )
                    ) then 0 else 1 end,
                    case when exists (
                        select 1 from borrowing b
                        where b.physical_item_id = p.id
                          and b.returned_at is null
                          and b.due_at < :now
                    ) and p.inventory_status <> 'WITHDRAWN' then 0 else 1 end,
                    case when p.inventory_status in ('LOST', 'DAMAGED')
                         and p.inventory_status <> 'WITHDRAWN' then 0 else 1 end,
                    case when (
                        p.location is null
                        or trim(p.location) = ''
                        or upper(trim(p.location)) = 'UNASSIGNED'
                    ) and p.inventory_status <> 'WITHDRAWN' then 0 else 1 end,
                    p.id asc
                """,
        countQuery = """
                select count(*)
                from physical_item p
                join resource r on r.id = p.resource_id
                where (:inventoryStatus = '' or p.inventory_status = :inventoryStatus)
                  and (:circulationStatus = '' or p.circulation_status = :circulationStatus)
                  and (
                        :q = ''
                        or lower(p.barcode) like lower(concat('%', :q, '%'))
                        or lower(p.location) like lower(concat('%', :q, '%'))
                        or lower(r.title) like lower(concat('%', :q, '%'))
                        or lower(r.authors) like lower(concat('%', :q, '%'))
                  )
                  and (
                        :attentionMode = -1
                        or (
                            :attentionMode = 1
                            and p.inventory_status <> 'WITHDRAWN'
                            and (
                                p.inventory_status in ('LOST', 'DAMAGED')
                                or p.location is null
                                or trim(p.location) = ''
                                or upper(trim(p.location)) = 'UNASSIGNED'
                                or exists (
                                    select 1 from borrowing b
                                    where b.physical_item_id = p.id
                                      and b.returned_at is null
                                      and b.due_at < :now
                                )
                                or (
                                    exists (
                                        select 1 from borrowing b
                                        where b.physical_item_id = p.id
                                          and b.returned_at is null
                                    )
                                    and exists (
                                        select 1 from borrow_request br
                                        where br.physical_item_id = p.id
                                          and br.status in ('REQUESTED', 'READY_FOR_PICKUP')
                                    )
                                )
                                or (
                                    p.circulation_status = 'RESERVED'
                                    and not exists (
                                        select 1 from borrow_request br
                                        where br.physical_item_id = p.id
                                          and br.status in ('REQUESTED', 'READY_FOR_PICKUP')
                                    )
                                )
                                or (
                                    p.circulation_status = 'BORROWED'
                                    and not exists (
                                        select 1 from borrowing b
                                        where b.physical_item_id = p.id
                                          and b.returned_at is null
                                    )
                                )
                                or (
                                    p.circulation_status = 'AVAILABLE'
                                    and (
                                        exists (
                                            select 1 from borrowing b
                                            where b.physical_item_id = p.id
                                              and b.returned_at is null
                                        )
                                        or exists (
                                            select 1 from borrow_request br
                                            where br.physical_item_id = p.id
                                              and br.status in ('REQUESTED', 'READY_FOR_PICKUP')
                                        )
                                    )
                                )
                            )
                        )
                        or (
                            :attentionMode = 0
                            and (
                                p.inventory_status = 'WITHDRAWN'
                                or not (
                                    p.inventory_status in ('LOST', 'DAMAGED')
                                    or p.location is null
                                    or trim(p.location) = ''
                                    or upper(trim(p.location)) = 'UNASSIGNED'
                                    or exists (
                                        select 1 from borrowing b
                                        where b.physical_item_id = p.id
                                          and b.returned_at is null
                                          and b.due_at < :now
                                    )
                                    or (
                                        exists (
                                            select 1 from borrowing b
                                            where b.physical_item_id = p.id
                                              and b.returned_at is null
                                        )
                                        and exists (
                                            select 1 from borrow_request br
                                            where br.physical_item_id = p.id
                                              and br.status in ('REQUESTED', 'READY_FOR_PICKUP')
                                        )
                                    )
                                    or (
                                        p.circulation_status = 'RESERVED'
                                        and not exists (
                                            select 1 from borrow_request br
                                            where br.physical_item_id = p.id
                                              and br.status in ('REQUESTED', 'READY_FOR_PICKUP')
                                        )
                                    )
                                    or (
                                        p.circulation_status = 'BORROWED'
                                        and not exists (
                                            select 1 from borrowing b
                                            where b.physical_item_id = p.id
                                              and b.returned_at is null
                                        )
                                    )
                                    or (
                                        p.circulation_status = 'AVAILABLE'
                                        and (
                                            exists (
                                                select 1 from borrowing b
                                                where b.physical_item_id = p.id
                                                  and b.returned_at is null
                                            )
                                            or exists (
                                                select 1 from borrow_request br
                                                where br.physical_item_id = p.id
                                                  and br.status in ('REQUESTED', 'READY_FOR_PICKUP')
                                            )
                                        )
                                    )
                                )
                            )
                        )
                  )
                """,
        nativeQuery = true
)
org.springframework.data.domain.Page<PhysicalItem> findCockpitItemsPaged(
        @Param("q") String q,
        @Param("inventoryStatus") String inventoryStatus,
        @Param("circulationStatus") String circulationStatus,
        @Param("attentionMode") int attentionMode,
        @Param("now") java.time.LocalDateTime now,
        Pageable pageable
);

    interface CockpitSummaryProjection {
        Long getTotalCopies();
        Long getAvailableCopies();
        Long getReservedCopies();
        Long getBorrowedCopies();
        Long getAttentionCopies();
    }

    @Query("""
            select
              sum(case when p.inventoryStatus <> com.librio.domain.InventoryStatus.WITHDRAWN then 1L else 0L end) as totalCopies,
              sum(case when p.inventoryStatus = com.librio.domain.InventoryStatus.ACTIVE and p.circulationStatus = com.librio.domain.CirculationStatus.AVAILABLE then 1L else 0L end) as availableCopies,
              sum(case when p.inventoryStatus <> com.librio.domain.InventoryStatus.WITHDRAWN and p.circulationStatus = com.librio.domain.CirculationStatus.RESERVED then 1L else 0L end) as reservedCopies,
              sum(case when p.inventoryStatus <> com.librio.domain.InventoryStatus.WITHDRAWN and p.circulationStatus = com.librio.domain.CirculationStatus.BORROWED then 1L else 0L end) as borrowedCopies,
              sum(case when p.inventoryStatus <> com.librio.domain.InventoryStatus.WITHDRAWN and (
                    p.inventoryStatus in (com.librio.domain.InventoryStatus.LOST, com.librio.domain.InventoryStatus.DAMAGED)
                    or (p.location is null or trim(p.location) = '' or upper(trim(p.location)) = 'UNASSIGNED')
                    or exists (select 1 from Borrowing b where b.physicalItem = p and b.returnedAt is null and b.dueAt < :now)
                    or (exists (select 1 from Borrowing b where b.physicalItem = p and b.returnedAt is null) and exists (select 1 from BorrowRequest br where br.physicalItem = p and br.status in (com.librio.domain.BorrowRequestStatus.REQUESTED, com.librio.domain.BorrowRequestStatus.READY_FOR_PICKUP)))
                    or (p.circulationStatus = com.librio.domain.CirculationStatus.RESERVED and not exists (select 1 from BorrowRequest br where br.physicalItem = p and br.status in (com.librio.domain.BorrowRequestStatus.REQUESTED, com.librio.domain.BorrowRequestStatus.READY_FOR_PICKUP)))
                    or (p.circulationStatus = com.librio.domain.CirculationStatus.BORROWED and not exists (select 1 from Borrowing b where b.physicalItem = p and b.returnedAt is null))
                    or (p.circulationStatus = com.librio.domain.CirculationStatus.AVAILABLE and (exists (select 1 from Borrowing b where b.physicalItem = p and b.returnedAt is null) or exists (select 1 from BorrowRequest br where br.physicalItem = p and br.status in (com.librio.domain.BorrowRequestStatus.REQUESTED, com.librio.domain.BorrowRequestStatus.READY_FOR_PICKUP))))
              ) then 1L else 0L end) as attentionCopies
            from PhysicalItem p
            """)
    CockpitSummaryProjection findCockpitSummary(@Param("now") java.time.LocalDateTime now);
}
