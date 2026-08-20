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
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String authors;

    @Column(columnDefinition = "TEXT")
    private String description;

    @OneToMany(mappedBy = "resource", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PhysicalItem> physicalItems = new ArrayList<>();

    @OneToOne(mappedBy = "resource", cascade = CascadeType.ALL, orphanRemoval = true)
    private DigitalItem digitalItem;
}
