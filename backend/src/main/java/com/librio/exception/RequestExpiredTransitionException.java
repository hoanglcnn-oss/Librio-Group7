package com.librio.exception;

import org.springframework.http.HttpStatus;

public class RequestExpiredTransitionException extends BorrowFlowException {
    public RequestExpiredTransitionException(String message) {
        super(BorrowErrorCode.REQUEST_EXPIRED.name(), HttpStatus.CONFLICT, message);
    }
}
