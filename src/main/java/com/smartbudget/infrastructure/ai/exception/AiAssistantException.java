package com.smartbudget.infrastructure.ai.exception;

public class AiAssistantException extends RuntimeException {
    public AiAssistantException(String message, Throwable cause) {
        super(message, cause);
    }

    public AiAssistantException(String message) {
        super(message);
    }
}
