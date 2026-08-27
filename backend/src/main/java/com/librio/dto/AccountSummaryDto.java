package com.librio.dto;

import com.librio.domain.AccountRole;
import com.librio.domain.AccountStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AccountSummaryDto {
    private Long id;
    private String email;
    private String displayName;
    private AccountRole role;
    private AccountStatus accountStatus;
}
