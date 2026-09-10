package com.librio.service;

import com.librio.dto.BookMetadataDto;
import com.librio.exception.IsbnLookupException;
import com.librio.provider.googlebooks.GoogleBooksAdapter;
import com.librio.util.IsbnUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BookMetadataLookupService {

    private final GoogleBooksAdapter googleBooksAdapter;

    public BookMetadataDto lookup(String isbn) {
        String normalizedIsbn13;
        try {
            normalizedIsbn13 = IsbnUtils.normalizeToIsbn13(isbn);
        } catch (IllegalArgumentException ex) {
            throw new IsbnLookupException(HttpStatus.BAD_REQUEST, "INVALID_ISBN", "Invalid ISBN format");
        }
        
        return googleBooksAdapter.findExactMatch(normalizedIsbn13);
    }
}
