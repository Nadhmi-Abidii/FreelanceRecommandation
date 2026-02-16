package com.towork.exception;

public class MilestoneNotFoundException extends ResourceNotFoundException {
    public MilestoneNotFoundException(String message) {
        super(message);
    }
}
