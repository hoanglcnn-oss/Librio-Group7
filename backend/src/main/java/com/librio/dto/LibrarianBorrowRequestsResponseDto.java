package com.librio.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class LibrarianBorrowRequestsResponseDto {
    private List<LibrarianBorrowRequestItemDto> items;
}
