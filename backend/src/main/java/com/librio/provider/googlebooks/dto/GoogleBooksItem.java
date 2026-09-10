package com.librio.provider.googlebooks.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleBooksItem {
    private String id;
    private VolumeInfo volumeInfo;
}
