package com.smartbudget.infrastructure.ai.exception;

public class SpeechSynthesisException extends RuntimeException {
    public SpeechSynthesisException(String message, Throwable cause) {
        super(message, cause);
    }
}
