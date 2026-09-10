package com.librio.service;

import com.librio.dto.BookMetadataDto;
import com.librio.exception.IsbnLookupException;
import com.librio.provider.googlebooks.GoogleBooksAdapter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BookMetadataLookupServiceTest {

    @Mock
    private GoogleBooksAdapter googleBooksAdapter;

    @InjectMocks
    private BookMetadataLookupService service;

    @Test
    void lookup_validIsbn13_callsAdapter() {
        BookMetadataDto dto = BookMetadataDto.builder().isbn("9780132350884").build();
        when(googleBooksAdapter.findExactMatch("9780132350884")).thenReturn(dto);

        BookMetadataDto result = service.lookup("978-0-13-235088-4");

        assertThat(result).isSameAs(dto);
        verify(googleBooksAdapter).findExactMatch("9780132350884");
    }

    @Test
    void lookup_invalidIsbn_throwsException() {
        IsbnLookupException ex = assertThrows(IsbnLookupException.class, () -> service.lookup("invalid"));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getCode()).isEqualTo("INVALID_ISBN");
        verifyNoInteractions(googleBooksAdapter);
    }
}
