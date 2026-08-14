package com.smartbudget.infrastructure.ai.service;

import com.smartbudget.infrastructure.ai.exception.SpeechSynthesisException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.audio.tts.Speech;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.audio.tts.TextToSpeechResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@DisplayName("SpeechService - Text to Speech")
class SpeechServiceTest {
    private TextToSpeechModel textToSpeechModel;
    private SpeechService service;

    @BeforeEach
    void setUp() {
        textToSpeechModel = mock(TextToSpeechModel.class);
        service = new SpeechService(textToSpeechModel);
    }

    @Test
    @DisplayName("devolve os bytes do audio gerado")
    void synthesizesAudio() {
        byte[] expected = {1, 2, 3, 4};
        given(textToSpeechModel.call(any(TextToSpeechPrompt.class)))
                .willReturn(new TextToSpeechResponse(List.of(new Speech(expected))));

        assertThat(service.synthesize("Seu saldo e de R$ 3.860,00.")).isEqualTo(expected);
    }

    @Test
    @DisplayName("texto vazio nao chega ao provedor")
    void rejectsBlankText() {
        assertThatThrownBy(() -> service.synthesize("  "))
                .isInstanceOf(SpeechSynthesisException.class);
    }

    @Test
    @DisplayName("audio vazio do provedor vira erro explicito")
    void rejectsEmptyAudio() {
        given(textToSpeechModel.call(any(TextToSpeechPrompt.class)))
                .willReturn(new TextToSpeechResponse(List.of(new Speech(new byte[0]))));

        assertThatThrownBy(() -> service.synthesize("qualquer coisa"))
                .isInstanceOf(SpeechSynthesisException.class);
    }

    @Test
    @DisplayName("falha do provedor e encapsulada")
    void wrapsProviderFailure() {
        given(textToSpeechModel.call(any(TextToSpeechPrompt.class)))
                .willThrow(new RuntimeException("timeout"));

        assertThatThrownBy(() -> service.synthesize("texto"))
                .isInstanceOf(SpeechSynthesisException.class)
                .hasRootCauseMessage("timeout");
    }
}
