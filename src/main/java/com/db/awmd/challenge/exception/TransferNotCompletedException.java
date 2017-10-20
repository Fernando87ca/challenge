package com.db.awmd.challenge.exception;

/**
 * Exception when is not possible complete the transaction with unexpected reason.
 */
public class TransferNotCompletedException extends RuntimeException {

    public TransferNotCompletedException(String message) {
        super(message);
    }
}
