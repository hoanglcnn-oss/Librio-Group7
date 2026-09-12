package com.librio.controller;

import com.librio.dto.DigitalAccessDto;
import com.librio.service.DigitalAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
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
    public ResponseEntity<DigitalAccessDto> capability(@PathVariable Long resourceId, Authentication authentication) {
        String email = null;
        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
            email = authentication.getName();
        }
        return ResponseEntity.ok(digitalAccessService.getCapability(resourceId, email));
    }

    @GetMapping(value = "/digital-preview", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> preview(@PathVariable Long resourceId) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename("librio-preview-" + resourceId + ".pdf").build().toString())
                .body(digitalAccessService.getDemoPdfPreview(resourceId));
    }

    @GetMapping(value = "/digital-content", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> content(@PathVariable Long resourceId, Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename("librio-resource-" + resourceId + ".pdf").build().toString())
                .body(digitalAccessService.getDemoPdfContent(resourceId, email));
    }
}
