package com.librio.repository;

import com.librio.domain.DigitalItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DigitalItemRepository extends JpaRepository<DigitalItem, Long> {

    Optional<DigitalItem> findByResourceId(Long resourceId);

    boolean existsByResourceId(Long resourceId);
}
