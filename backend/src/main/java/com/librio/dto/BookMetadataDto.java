package com.librio.dto;

import com.librio.domain.MetadataSource;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BookMetadataDto {
    private String title;
    private List<String> authors;
    private String description;
    private String isbn;
    private String coverImageUrl;
    private String externalSourceId;
    private MetadataSource metadataSource;
}
