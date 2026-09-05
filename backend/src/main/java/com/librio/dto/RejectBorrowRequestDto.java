package com.librio.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RejectBorrowRequestDto {
    @Size(max = 500)
    private String reason;
}
