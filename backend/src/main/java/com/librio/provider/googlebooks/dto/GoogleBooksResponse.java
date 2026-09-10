package com.librio.provider.googlebooks.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleBooksResponse {
    private String kind;
    private int totalItems;
    private List<GoogleBooksItem> items;
}
