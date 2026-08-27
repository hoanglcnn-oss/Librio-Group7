package com.librio.exception;

import com.librio.dto.ErrorResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BorrowFlowException.class)
    public ResponseEntity<ErrorResponseDto> handleBorrowFlow(BorrowFlowException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(new ErrorResponseDto(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleResourceNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponseDto("RESOURCE_NOT_FOUND", ex.getMessage()));
    }
}
