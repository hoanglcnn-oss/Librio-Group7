package com.librio.dto;

import com.librio.domain.CirculationStatus;
import com.librio.domain.InventoryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CockpitPhysicalItemDto {
    private Long id;
    private String barcode;
    private String location;
    private InventoryStatus inventoryStatus;
    private CirculationStatus circulationStatus;
    private boolean borrowable;
    private boolean needsAttention;
    private List<String> attentionReasons;
    private CockpitResourceDto resource;
    private CockpitActiveOperationDto activeOperation;
}
