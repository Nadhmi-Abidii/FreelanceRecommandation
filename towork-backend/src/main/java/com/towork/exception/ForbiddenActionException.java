package com.towork.exception;

public class ForbiddenActionException extends BusinessException {
    public ForbiddenActionException(String message) {
        super(message);
    }
}
