package com.smartbudget.infrastructure.ai.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Validacao da chave da OpenAI na inicializacao")
class ChatClientConfigTest {
    @ParameterizedTest(name = "chave \"{0}\" impede a aplicacao de iniciar")
    @ValueSource(strings = {"", "   "})
    @DisplayName("chave vazia interrompe a inicializacao")
    void rejectsBlankKey(String apiKey) {
        assertThatThrownBy(() -> ChatClientConfig.requireApiKey(apiKey))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OPENAI_API_KEY");
    }

    @Test
    @DisplayName("chave nula interrompe a inicializacao")
    void rejectsNullKey() {
        assertThatThrownBy(() -> ChatClientConfig.requireApiKey(null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("chave presente permite a inicializacao")
    void acceptsConfiguredKey() {
        assertThatCode(() -> ChatClientConfig.requireApiKey("uma-chave-qualquer")).doesNotThrowAnyException();
    }
}
