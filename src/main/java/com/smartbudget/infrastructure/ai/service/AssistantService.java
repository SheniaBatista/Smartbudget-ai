package com.smartbudget.infrastructure.ai.service;

import com.smartbudget.domain.exception.DomainException;
import com.smartbudget.infrastructure.ai.exception.AiAssistantException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;

@Service
public class AssistantService {
    private static final Logger log = LoggerFactory.getLogger(AssistantService.class);
    private static final Locale PT_BR = Locale.of("pt", "BR");

    private final ChatClient chatClient;
    private final String systemPrompt;

    public AssistantService(ChatClient financeChatClient,
                            @Value("classpath:prompts/assistant-system.txt") Resource systemPromptResource) {
        this.chatClient = financeChatClient;
        this.systemPrompt = readPrompt(systemPromptResource);
    }

    public String reply(String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("A mensagem enviada ao assistente nao pode ser vazia.");
        }

        try {
            String answer = chatClient.prompt()
                    .system(systemPrompt + temporalContext())
                    .user(message.trim())
                    .call()
                    .content();

            log.info("AI request processed");
            return answer != null ? answer.trim() : "";

        } catch (DomainException | AiAssistantException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AiAssistantException("Falha ao consultar o provedor de IA.", exception);
        }
    }

    private String temporalContext() {
        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(today);

        return """

                CONTEXTO TEMPORAL
                Data de hoje: %s (%s).
                Ontem: %s.
                Mes atual: %s, que vai de %s ate %s.
                Mes anterior: %s.""".formatted(
                today,
                today.getDayOfWeek().getDisplayName(TextStyle.FULL, PT_BR),
                today.minusDays(1),
                currentMonth,
                currentMonth.atDay(1),
                currentMonth.atEndOfMonth(),
                currentMonth.minusMonths(1));
    }

    private static String readPrompt(Resource resource) {
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException("Nao foi possivel carregar o prompt de sistema do assistente.", exception);
        }
    }
}
