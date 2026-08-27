package com.librio.exception;

import org.springframework.http.HttpStatus;

public class BorrowFlowException extends RuntimeException {
    private final String code;
    private final HttpStatus status;

    public BorrowFlowException(String code, HttpStatus status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public BorrowFlowException(HttpStatus status, String message) {
        this(null, status, message);
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
