package com.librio.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResourceDetailDto {
    private Long id;
    private String title;
    private List<String> authors;
    private String description;
    private List<String> accessTypes;
    private PhysicalAvailabilityDto physical;
    private DigitalAvailabilityDto digital;
}
