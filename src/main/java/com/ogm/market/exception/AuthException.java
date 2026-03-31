package com.ogm.market.exception;

/** Thrown when authentication fails (wrong password, user not found, etc.) */
public class AuthException extends RuntimeException {
    public AuthException(String message) { super(message); }
}