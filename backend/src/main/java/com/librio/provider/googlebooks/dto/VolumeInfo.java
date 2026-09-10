package com.librio.provider.googlebooks.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class VolumeInfo {
    private String title;
    private List<String> authors;
    private String description;
    private List<IndustryIdentifier> industryIdentifiers;
    private ImageLinks imageLinks;
}
