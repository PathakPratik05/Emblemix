package com.emblemix.common.exception;

import org.springframework.http.HttpStatus;

public class SsrfProtectionException extends ApiException {
    public SsrfProtectionException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
