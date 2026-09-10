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

    @Query("""
            select p from PhysicalItem p
            join fetch p.resource r
            where (:inventoryStatus is null or p.inventoryStatus = :inventoryStatus)
              and (:circulationStatus is null or p.circulationStatus = :circulationStatus)
              and (:q is null or :q = '' or (
                    lower(p.barcode) like lower(concat('%', :q, '%')) or
                    lower(p.location) like lower(concat('%', :q, '%')) or
                    lower(r.title) like lower(concat('%', :q, '%')) or
                    lower(r.authors) like lower(concat('%', :q, '%'))
              ))
            """)
    List<PhysicalItem> findCockpitItems(
            @Param("q") String q,
            @Param("inventoryStatus") InventoryStatus inventoryStatus,
            @Param("circulationStatus") CirculationStatus circulationStatus
    );
}
