package com.db.awmd.challenge.exception;

public class InsufficientAmountException extends RuntimeException {
    public InsufficientAmountException(String message) {
        super(message);
    }
}
