package com.librio.provider.googlebooks;

import com.librio.domain.MetadataSource;
import com.librio.dto.BookMetadataDto;
import com.librio.exception.IsbnLookupException;
import com.librio.provider.googlebooks.dto.GoogleBooksItem;
import com.librio.provider.googlebooks.dto.GoogleBooksResponse;
import com.librio.provider.googlebooks.dto.IndustryIdentifier;
import com.librio.provider.googlebooks.dto.VolumeInfo;
import com.librio.provider.googlebooks.dto.ImageLinks;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.ResourceAccessException;

import java.net.SocketTimeoutException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GoogleBooksAdapterTest {

    @Mock
    private GoogleBooksClient client;

    @InjectMocks
    private GoogleBooksAdapter adapter;

    @Test
    void findExactMatch_exactMatch_success() {
        GoogleBooksResponse response = new GoogleBooksResponse();
        GoogleBooksItem item = new GoogleBooksItem();
        item.setId("google-id-123");
        VolumeInfo volumeInfo = new VolumeInfo();
        volumeInfo.setTitle("Test Book");
        volumeInfo.setAuthors(List.of("Author 1"));
        volumeInfo.setDescription("Desc");
        
        IndustryIdentifier id1 = new IndustryIdentifier();
        id1.setType("ISBN_13");
        id1.setIdentifier("9781234567890");
        volumeInfo.setIndustryIdentifiers(List.of(id1));
        
        ImageLinks links = new ImageLinks();
        links.setThumbnail("http://example.com/thumb.jpg");
        volumeInfo.setImageLinks(links);
        
        item.setVolumeInfo(volumeInfo);
        response.setItems(List.of(item));

        when(client.lookupByIsbn("9781234567890")).thenReturn(response);

        BookMetadataDto dto = adapter.findExactMatch("9781234567890");

        assertThat(dto.getTitle()).isEqualTo("Test Book");
        assertThat(dto.getAuthors()).containsExactly("Author 1");
        assertThat(dto.getDescription()).isEqualTo("Desc");
        assertThat(dto.getIsbn()).isEqualTo("9781234567890");
        assertThat(dto.getCoverImageUrl()).isEqualTo("https://example.com/thumb.jpg");
        assertThat(dto.getExternalSourceId()).isEqualTo("google-id-123");
        assertThat(dto.getMetadataSource()).isEqualTo(MetadataSource.GOOGLE_BOOKS);
    }

    @Test
    void findExactMatch_noMatchInResults_throwsNotFound() {
        GoogleBooksResponse response = new GoogleBooksResponse();
        GoogleBooksItem item = new GoogleBooksItem();
        VolumeInfo volumeInfo = new VolumeInfo();
        
        IndustryIdentifier id1 = new IndustryIdentifier();
        id1.setType("ISBN_13");
        id1.setIdentifier("9789999999999"); // Different ISBN
        volumeInfo.setIndustryIdentifiers(List.of(id1));
        
        item.setVolumeInfo(volumeInfo);
        response.setItems(List.of(item));

        when(client.lookupByIsbn("9781234567890")).thenReturn(response);

        IsbnLookupException ex = assertThrows(IsbnLookupException.class, () -> adapter.findExactMatch("9781234567890"));
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getCode()).isEqualTo("BOOK_METADATA_NOT_FOUND");
    }

    @Test
    void findExactMatch_emptyItems_throwsNotFound() {
        GoogleBooksResponse response = new GoogleBooksResponse();
        response.setItems(List.of()); // Empty

        when(client.lookupByIsbn("9781234567890")).thenReturn(response);

        IsbnLookupException ex = assertThrows(IsbnLookupException.class, () -> adapter.findExactMatch("9781234567890"));
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getCode()).isEqualTo("BOOK_METADATA_NOT_FOUND");
    }

    @Test
    void findExactMatch_timeout_throwsGatewayTimeout() {
        when(client.lookupByIsbn("9781234567890")).thenThrow(new ResourceAccessException("Timeout", new SocketTimeoutException("Read timed out")));

        IsbnLookupException ex = assertThrows(IsbnLookupException.class, () -> adapter.findExactMatch("9781234567890"));
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
        assertThat(ex.getCode()).isEqualTo("BOOK_METADATA_LOOKUP_TIMEOUT");
    }

    @Test
    void findExactMatch_providerError_throwsBadGateway() {
        when(client.lookupByIsbn("9781234567890")).thenThrow(new RuntimeException("Server error"));

        IsbnLookupException ex = assertThrows(IsbnLookupException.class, () -> adapter.findExactMatch("9781234567890"));
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(ex.getCode()).isEqualTo("BOOK_METADATA_PROVIDER_ERROR");
    }
    
    @Test
    void findExactMatch_missingOptionalFields_success() {
        GoogleBooksResponse response = new GoogleBooksResponse();
        GoogleBooksItem item = new GoogleBooksItem();
        item.setId("google-id-123");
        VolumeInfo volumeInfo = new VolumeInfo();
        volumeInfo.setTitle("Test Book");
        // Authors, description, imageLinks are null
        
        IndustryIdentifier id1 = new IndustryIdentifier();
        id1.setType("ISBN_13");
        id1.setIdentifier("9781234567890");
        volumeInfo.setIndustryIdentifiers(List.of(id1));
        
        item.setVolumeInfo(volumeInfo);
        response.setItems(List.of(item));

        when(client.lookupByIsbn("9781234567890")).thenReturn(response);

        BookMetadataDto dto = adapter.findExactMatch("9781234567890");

        assertThat(dto.getTitle()).isEqualTo("Test Book");
        assertThat(dto.getAuthors()).isNull();
        assertThat(dto.getDescription()).isNull();
        assertThat(dto.getCoverImageUrl()).isNull();
    }
}