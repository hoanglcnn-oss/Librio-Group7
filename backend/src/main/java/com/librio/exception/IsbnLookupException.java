package com.librio.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class IsbnLookupException extends RuntimeException {
    
    private final HttpStatus status;
    private final String code;

    public IsbnLookupException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
}
