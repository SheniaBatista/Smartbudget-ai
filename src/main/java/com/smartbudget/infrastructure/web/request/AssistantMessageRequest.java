package com.smartbudget.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AssistantMessageRequest(

        @NotBlank(message = "A mensagem e obrigatoria.")
        @Size(max = 1000, message = "A mensagem nao pode ultrapassar 1000 caracteres.")
        String message) {
}
