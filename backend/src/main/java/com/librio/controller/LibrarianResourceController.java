package com.librio.controller;

import com.librio.dto.ManagedResourceDto;
import com.librio.dto.ResourceAdminRequestDto;
import com.librio.service.ResourceAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/librarian/resources")
@RequiredArgsConstructor
public class LibrarianResourceController {
    private final ResourceAdminService resourceAdminService;

    @GetMapping("/{id}")
    public ResponseEntity<ManagedResourceDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(resourceAdminService.get(id));
    }

    @PostMapping
    public ResponseEntity<ManagedResourceDto> create(@Valid @RequestBody ResourceAdminRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(resourceAdminService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ManagedResourceDto> update(
            @PathVariable Long id,
            @Valid @RequestBody ResourceAdminRequestDto request) {
        return ResponseEntity.ok(resourceAdminService.update(id, request));
    }
}
