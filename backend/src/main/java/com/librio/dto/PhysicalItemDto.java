package com.librio.dto;

import com.librio.domain.CirculationStatus;
import com.librio.domain.InventoryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhysicalItemDto {

    private Long id;
    private Long resourceId;
    private String barcode;
    private String location;
    private InventoryStatus inventoryStatus;
    private CirculationStatus circulationStatus;
}
