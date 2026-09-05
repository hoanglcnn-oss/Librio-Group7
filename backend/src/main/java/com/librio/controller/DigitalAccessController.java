package com.librio.controller;

import com.librio.dto.DigitalAccessDto;
import com.librio.service.DigitalAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/resources/{resourceId}")
@RequiredArgsConstructor
public class DigitalAccessController {
    private final DigitalAccessService digitalAccessService;

    @GetMapping("/digital-access")
    public ResponseEntity<DigitalAccessDto> capability(@PathVariable Long resourceId) {
        return ResponseEntity.ok(digitalAccessService.getCapability(resourceId));
    }

    @GetMapping(value = "/digital-content", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> content(@PathVariable Long resourceId) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename("librio-resource-" + resourceId + ".pdf").build().toString())
                .body(digitalAccessService.getDemoPdf(resourceId));
    }
}
