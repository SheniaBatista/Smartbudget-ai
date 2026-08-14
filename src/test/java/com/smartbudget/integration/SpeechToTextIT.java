package com.smartbudget.integration;

import com.smartbudget.infrastructure.ai.service.TranscriptionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
@DisplayName("[IT] Speech-to-Text com a OpenAI")
class SpeechToTextIT {
    @Autowired
    private TranscriptionService transcriptionService;

    @ParameterizedTest(name = "{0} deve conter \"{1}\"")
    @CsvSource({
            "comando-1-despesa-uber.mp3,      uber",
            "comando-2-receita-salario.mp3,   sal",
            "comando-3-consulta-saldo.mp3,    saldo",
            "comando-4-gasto-categoria.mp3,   aliment",
            "comando-5-resumo-mensal.mp3,     resumo"
    })
    @DisplayName("transcreve comandos financeiros em portugues")
    void transcribesPortugueseCommands(String fileName, String expectedFragment) throws IOException {
        var audio = new ClassPathResource("audio/" + fileName);

        var upload = new MockMultipartFile("file", fileName, "audio/mpeg", audio.getContentAsByteArray());
        String transcription = transcriptionService.transcribe(upload);

        assertThat(transcription).isNotBlank();
        assertThat(transcription.toLowerCase(Locale.ROOT)).contains(expectedFragment);
    }
}
