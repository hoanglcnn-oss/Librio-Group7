package com.librio.controller;

import com.librio.dto.BookMetadataDto;
import com.librio.service.BookMetadataLookupService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class LibrarianBookMetadataControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookMetadataLookupService service;

    @Test
    void lookup_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/librarian/book-metadata/lookup").param("isbn", "9780132350884"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "READER")
    void lookup_reader_returns403() throws Exception {
        mockMvc.perform(get("/librarian/book-metadata/lookup").param("isbn", "9780132350884"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void lookup_librarian_returns200() throws Exception {
        when(service.lookup(anyString())).thenReturn(BookMetadataDto.builder().isbn("9780132350884").build());
        
        mockMvc.perform(get("/librarian/book-metadata/lookup").param("isbn", "9780132350884"))
                .andExpect(status().isOk());
    }
}