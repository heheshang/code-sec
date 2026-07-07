package com.codesec.common.exception;

/**
 * Thrown when a client request is malformed or semantically invalid.
 * Maps to HTTP 400.
 */
public class BadRequestException extends BizException {
    public BadRequestException(String message) {
        super(400, message);
    }
}
