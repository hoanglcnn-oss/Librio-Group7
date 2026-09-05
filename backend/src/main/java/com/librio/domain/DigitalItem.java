package com.librio.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "digital_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DigitalItem {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "digital_item_seq")
    @SequenceGenerator(name = "digital_item_seq", sequenceName = "digital_item_id_seq", allocationSize = 1, initialValue = 1000)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id", nullable = false)
    private Resource resource;
}
