package com.librio.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateBorrowRequestDto {
    @NotNull
    private Long resourceId;
}
