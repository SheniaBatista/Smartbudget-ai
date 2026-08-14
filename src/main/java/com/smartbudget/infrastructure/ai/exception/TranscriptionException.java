package com.smartbudget.infrastructure.ai.exception;

public class TranscriptionException extends RuntimeException {
    public TranscriptionException(String message, Throwable cause) {
        super(message, cause);
    }

    public TranscriptionException(String message) {
        super(message);
    }
}
