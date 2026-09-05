package com.librio.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ManagedResourceDto {
    private Long id;
    private String title;
    private List<String> authors;
    private String description;
    private String category;
    private List<String> accessTypes;
    private PhysicalAvailabilityDto physical;
    private DigitalAvailabilityDto digital;
}
