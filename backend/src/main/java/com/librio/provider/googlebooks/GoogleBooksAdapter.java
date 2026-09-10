package com.librio.provider.googlebooks;

import com.librio.domain.MetadataSource;
import com.librio.dto.BookMetadataDto;
import com.librio.exception.IsbnLookupException;
import com.librio.provider.googlebooks.dto.GoogleBooksItem;
import com.librio.provider.googlebooks.dto.GoogleBooksResponse;
import com.librio.provider.googlebooks.dto.IndustryIdentifier;
import com.librio.provider.googlebooks.dto.VolumeInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;

import java.net.SocketTimeoutException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GoogleBooksAdapter {

    private final GoogleBooksClient client;

    public BookMetadataDto findExactMatch(String normalizedIsbn13) {
        GoogleBooksResponse response;
        try {
            response = client.lookupByIsbn(normalizedIsbn13);
        } catch (ResourceAccessException e) {
            if (e.getCause() instanceof SocketTimeoutException || e.getCause() instanceof java.net.SocketTimeoutException) {
                throw new IsbnLookupException(HttpStatus.GATEWAY_TIMEOUT, "BOOK_METADATA_LOOKUP_TIMEOUT", "Timeout while communicating with Google Books");
            }
            throw new IsbnLookupException(HttpStatus.BAD_GATEWAY, "BOOK_METADATA_PROVIDER_ERROR", "Error communicating with Google Books");
        } catch (Exception e) {
            throw new IsbnLookupException(HttpStatus.BAD_GATEWAY, "BOOK_METADATA_PROVIDER_ERROR", "Error communicating with Google Books");
        }

        if (response == null || response.getItems() == null || response.getItems().isEmpty()) {
            throw new IsbnLookupException(HttpStatus.NOT_FOUND, "BOOK_METADATA_NOT_FOUND", "No metadata found for ISBN");
        }

        for (GoogleBooksItem item : response.getItems()) {
            VolumeInfo volumeInfo = item.getVolumeInfo();
            if (volumeInfo != null && volumeInfo.getIndustryIdentifiers() != null) {
                boolean exactMatch = volumeInfo.getIndustryIdentifiers().stream()
                        .anyMatch(identifier -> "ISBN_13".equals(identifier.getType()) && normalizedIsbn13.equals(identifier.getIdentifier()));

                if (exactMatch) {
                    return mapToDto(item, normalizedIsbn13);
                }
            }
        }

        throw new IsbnLookupException(HttpStatus.NOT_FOUND, "BOOK_METADATA_NOT_FOUND", "No exact metadata found for ISBN");
    }

    private BookMetadataDto mapToDto(GoogleBooksItem item, String normalizedIsbn13) {
        VolumeInfo volumeInfo = item.getVolumeInfo();
        
        String coverUrl = null;
        if (volumeInfo.getImageLinks() != null) {
            coverUrl = Optional.ofNullable(volumeInfo.getImageLinks().getThumbnail())
                    .orElse(volumeInfo.getImageLinks().getSmallThumbnail());
            
            if (coverUrl != null && coverUrl.startsWith("http:")) {
                coverUrl = coverUrl.replaceFirst("http:", "https:");
            }
        }

        return BookMetadataDto.builder()
                .title(volumeInfo.getTitle())
                .authors(volumeInfo.getAuthors())
                .description(volumeInfo.getDescription())
                .isbn(normalizedIsbn13)
                .coverImageUrl(coverUrl)
                .externalSourceId(item.getId())
                .metadataSource(MetadataSource.GOOGLE_BOOKS)
                .build();
    }
}
