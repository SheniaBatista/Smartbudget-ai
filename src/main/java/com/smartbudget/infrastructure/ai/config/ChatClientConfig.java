package com.smartbudget.infrastructure.ai.config;

import com.smartbudget.infrastructure.ai.tools.FinanceTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {
    @Bean
    public ChatClient financeChatClient(ChatClient.Builder builder,
                                        FinanceTools financeTools,
                                        @Value("${spring.ai.openai.api-key}") String apiKey) {
        requireApiKey(apiKey);
        return builder.defaultTools(financeTools).build();
    }

    static void requireApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("""
                    A variavel de ambiente OPENAI_API_KEY esta vazia.

                    Windows PowerShell:  $env:OPENAI_API_KEY="sua_chave"
                    Linux / macOS:       export OPENAI_API_KEY="sua_chave"

                    Os testes automatizados nao precisam dela.""");
        }
    }
}
