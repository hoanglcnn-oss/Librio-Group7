package com.librio.exception;

import org.springframework.http.HttpStatus;

public class BorrowFlowException extends RuntimeException {
    private final HttpStatus status;

    public BorrowFlowException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
