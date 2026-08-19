package com.librio.controller;

import com.librio.dto.ResourceDetailDto;
import com.librio.dto.ResourceListResponseDto;
import com.librio.service.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/resources")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;

    @GetMapping
    public ResponseEntity<ResourceListResponseDto> getResources(@RequestParam(value = "q", required = false) String q) {
        ResourceListResponseDto response = resourceService.searchResources(q);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResourceDetailDto> getResourceById(@PathVariable("id") Long id) {
        ResourceDetailDto response = resourceService.getResourceById(id);
        return ResponseEntity.ok(response);
    }
}
