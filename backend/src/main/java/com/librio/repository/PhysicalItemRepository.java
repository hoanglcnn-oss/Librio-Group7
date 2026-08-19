package com.librio.repository;

import com.librio.domain.PhysicalItem;
import com.librio.domain.PhysicalItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PhysicalItemRepository extends JpaRepository<PhysicalItem, Long> {

    List<PhysicalItem> findByResourceId(Long resourceId);

    long countByResourceId(Long resourceId);

    long countByResourceIdAndStatus(Long resourceId, PhysicalItemStatus status);
}
