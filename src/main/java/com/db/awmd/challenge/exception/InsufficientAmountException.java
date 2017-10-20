package com.db.awmd.challenge.exception;

/**
 * Exception when the user don't have enough money to perform the transaction.
 */
public class InsufficientAmountException extends RuntimeException {

    public InsufficientAmountException(String message) {
        super(message);
    }
}
