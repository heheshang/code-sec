package com.codesec.common.exception;

/**
 * Thrown when a requested resource does not exist.
 * Maps to HTTP 404.
 */
public class NotFoundException extends BizException {
    public NotFoundException(String message) {
        super(404, message);
    }
}
