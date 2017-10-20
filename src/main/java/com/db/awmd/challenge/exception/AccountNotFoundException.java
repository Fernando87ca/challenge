package com.db.awmd.challenge.exception;

/**
 * Exception when is not possible find the user in Account Repository
 */
public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(String message) {
        super(message);
    }
}
