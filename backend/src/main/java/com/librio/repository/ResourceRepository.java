package com.librio.repository;

import com.librio.domain.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long> {

    @Query("SELECT r FROM Resource r WHERE LOWER(r.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(r.authors) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Resource> searchByKeyword(@Param("keyword") String keyword);
}
