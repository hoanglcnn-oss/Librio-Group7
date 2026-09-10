package com.librio.controller;

import com.librio.dto.BookMetadataDto;
import com.librio.service.BookMetadataLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/librarian/book-metadata")
@RequiredArgsConstructor
public class LibrarianBookMetadataController {

    private final BookMetadataLookupService bookMetadataLookupService;

    @GetMapping("/lookup")
    @PreAuthorize("hasRole('LIBRARIAN')")
    public BookMetadataDto lookup(@RequestParam("isbn") String isbn) {
        return bookMetadataLookupService.lookup(isbn);
    }
}
