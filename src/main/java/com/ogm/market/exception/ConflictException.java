package com.ogm.market.exception;

/** Thrown when a resource already exists (e.g. duplicate email on signup). */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) { super(message); }
}