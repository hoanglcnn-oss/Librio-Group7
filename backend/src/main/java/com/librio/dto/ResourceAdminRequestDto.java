package com.librio.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ResourceAdminRequestDto {
    @NotBlank
    @Size(max = 200)
    private String title;

    @NotEmpty
    private List<@NotBlank @Size(max = 255) String> authors;

    @Size(max = 5000)
    private String description;

    @Size(max = 100)
    private String category;

    @NotEmpty
    private List<String> accessTypes;

    @Valid
    private PhysicalInput physical;

    @Data
    public static class PhysicalInput {
        @Min(0)
        @Max(9999)
        private long totalCopies;
    }
}
