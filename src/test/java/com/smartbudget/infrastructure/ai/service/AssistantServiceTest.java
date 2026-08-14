package com.smartbudget.infrastructure.ai.service;

import com.smartbudget.infrastructure.ai.exception.AiAssistantException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.time.LocalDate;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DisplayName("AssistantService - pipeline de texto")
class AssistantServiceTest {
    private static final Resource SYSTEM_PROMPT = new ClassPathResource("prompts/assistant-system.txt");

    @Test
    @DisplayName("devolve a resposta do modelo sem espacos sobrando")
    void returnsTrimmedAnswer() {
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        given(chatClient.prompt().system(anyString()).user(anyString()).call().content())
                .willReturn("  Despesa registrada.  ");

        AssistantService service = new AssistantService(chatClient, SYSTEM_PROMPT);

        assertThat(service.reply("Registre 50 reais de Uber")).isEqualTo("Despesa registrada.");
    }

    @Test
    @DisplayName("envia o prompt de sistema junto com a data corrente")
    void sendsSystemPromptWithTemporalContext() {
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);

        AssistantService service = new AssistantService(chatClient, SYSTEM_PROMPT);
        service.reply("Qual e o meu saldo?");

        ArgumentCaptor<String> systemText = ArgumentCaptor.forClass(String.class);
        verify(chatClient.prompt()).system(systemText.capture());

        assertThat(systemText.getValue())
                .contains("SmartBudget AI")
                .contains("CONTEXTO TEMPORAL")
                .contains(LocalDate.now().toString())
                .contains(YearMonth.now().toString());
    }

    @Test
    @DisplayName("mensagem vazia e rejeitada antes de qualquer chamada ao modelo")
    void rejectsBlankMessage() {
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        AssistantService service = new AssistantService(chatClient, SYSTEM_PROMPT);

        assertThatThrownBy(() -> service.reply("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("falha do provedor e encapsulada em AiAssistantException")
    void wrapsProviderFailure() {
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        given(chatClient.prompt().system(anyString()).user(anyString()).call().content())
                .willThrow(new RuntimeException("429 Too Many Requests"));

        AssistantService service = new AssistantService(chatClient, SYSTEM_PROMPT);

        assertThatThrownBy(() -> service.reply("Resumo do mes"))
                .isInstanceOf(AiAssistantException.class)
                .hasRootCauseMessage("429 Too Many Requests");
    }
}
