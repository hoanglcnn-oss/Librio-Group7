package com.librio.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "physical_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhysicalItem {

    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id", nullable = false)
    private Resource resource;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PhysicalItemStatus status;
}
