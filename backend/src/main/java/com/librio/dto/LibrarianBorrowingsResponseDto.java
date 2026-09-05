package com.librio.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class LibrarianBorrowingsResponseDto {
    private List<LibrarianBorrowingDto> activeBorrowings;
}
