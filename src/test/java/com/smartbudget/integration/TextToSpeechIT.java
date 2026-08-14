package com.smartbudget.integration;

import com.smartbudget.infrastructure.ai.service.SpeechService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
@DisplayName("[IT] Text-to-Speech com a OpenAI")
class TextToSpeechIT {
    @Autowired
    private SpeechService speechService;

    @Test
    @DisplayName("gera um MP3 audivel a partir da resposta do assistente")
    void producesAudioFromText() {
        byte[] audio = speechService.synthesize(
                "Neste mes voce registrou mil e quarenta reais em despesas. "
                        + "A categoria com maior gasto foi alimentacao.");

        assertThat(audio).hasSizeGreaterThan(1024);

        boolean id3 = audio[0] == 'I' && audio[1] == 'D' && audio[2] == '3';
        boolean frameSync = (audio[0] & 0xFF) == 0xFF && (audio[1] & 0xE0) == 0xE0;
        assertThat(id3 || frameSync)
                .as("os bytes devem comecar como um MP3 valido")
                .isTrue();
    }
}
