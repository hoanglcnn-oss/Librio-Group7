package com.librio.controller;

import com.librio.domain.MetadataSource;
import com.librio.dto.BookMetadataDto;
import com.librio.exception.IsbnLookupException;
import com.librio.service.BookMetadataLookupService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LibrarianBookMetadataController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for simple testing
public class LibrarianBookMetadataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookMetadataLookupService service;

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void lookup_success() throws Exception {
        BookMetadataDto dto = BookMetadataDto.builder()
                .title("Clean Code")
                .authors(List.of("Robert C. Martin"))
                .isbn("9780132350884")
                .metadataSource(MetadataSource.GOOGLE_BOOKS)
                .build();

        when(service.lookup("978-0-13-235088-4")).thenReturn(dto);

        mockMvc.perform(get("/librarian/book-metadata/lookup").param("isbn", "978-0-13-235088-4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Clean Code"))
                .andExpect(jsonPath("$.isbn").value("9780132350884"))
                .andExpect(jsonPath("$.metadataSource").value("GOOGLE_BOOKS"));
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void lookup_notFound() throws Exception {
        when(service.lookup("9780000000000"))
                .thenThrow(new IsbnLookupException(HttpStatus.NOT_FOUND, "BOOK_METADATA_NOT_FOUND", "Not found"));

        mockMvc.perform(get("/librarian/book-metadata/lookup").param("isbn", "9780000000000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BOOK_METADATA_NOT_FOUND"));
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void lookup_timeout() throws Exception {
        when(service.lookup("9780132350884"))
                .thenThrow(new IsbnLookupException(HttpStatus.GATEWAY_TIMEOUT, "BOOK_METADATA_LOOKUP_TIMEOUT", "Timeout"));

        mockMvc.perform(get("/librarian/book-metadata/lookup").param("isbn", "9780132350884"))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.code").value("BOOK_METADATA_LOOKUP_TIMEOUT"));
    }
}
