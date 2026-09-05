package com.librio.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "resource")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "resource_seq")
    @SequenceGenerator(name = "resource_seq", sequenceName = "resource_id_seq", allocationSize = 1, initialValue = 1000)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String authors;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String category;

    @OneToMany(mappedBy = "resource", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PhysicalItem> physicalItems = new ArrayList<>();

    @OneToOne(mappedBy = "resource", cascade = CascadeType.ALL, orphanRemoval = true)
    private DigitalItem digitalItem;
}
