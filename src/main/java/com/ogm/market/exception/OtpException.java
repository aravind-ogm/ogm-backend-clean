package com.ogm.market.exception;

/** Thrown for invalid, expired, or missing OTPs. */
public class OtpException extends RuntimeException {
    public OtpException(String message) { super(message); }
}