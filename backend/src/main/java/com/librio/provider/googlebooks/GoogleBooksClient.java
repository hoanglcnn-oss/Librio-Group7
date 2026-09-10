package com.librio.provider.googlebooks;

import com.librio.provider.googlebooks.dto.GoogleBooksResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;

@Component
public class GoogleBooksClient {

    private final RestTemplate restTemplate;
    private final String apiUrl;

    public GoogleBooksClient(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${librio.google-books.api-url}") String apiUrl) {
        this.apiUrl = apiUrl;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
    }

    public GoogleBooksResponse lookupByIsbn(String isbn) {
        String url = UriComponentsBuilder.fromHttpUrl(apiUrl)
                .queryParam("q", "isbn:" + isbn)
                .toUriString();
        return restTemplate.getForObject(url, GoogleBooksResponse.class);
    }
}
