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
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id", nullable = false)
    private Resource resource;
}
