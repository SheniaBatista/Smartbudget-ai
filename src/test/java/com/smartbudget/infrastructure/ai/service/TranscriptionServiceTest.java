package com.smartbudget.infrastructure.ai.service;

import com.smartbudget.infrastructure.ai.config.AssistantProperties;
import com.smartbudget.infrastructure.ai.exception.InvalidAudioException;
import com.smartbudget.infrastructure.ai.exception.TranscriptionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.audio.transcription.AudioTranscription;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@DisplayName("TranscriptionService - Speech to Text")
class TranscriptionServiceTest {
    private TranscriptionModel transcriptionModel;
    private TranscriptionService service;

    @BeforeEach
    void setUp() {
        transcriptionModel = mock(TranscriptionModel.class);
        service = new TranscriptionService(transcriptionModel, new AssistantProperties());
    }

    @Test
    @DisplayName("converte o audio em texto")
    void transcribesAudio() {
        given(transcriptionModel.call(any(AudioTranscriptionPrompt.class)))
                .willReturn(new AudioTranscriptionResponse(
                        new AudioTranscription("  Registre uma despesa de 85 reais com Uber  ")));

        String text = service.transcribe(audioFile("comando.mp3"));

        assertThat(text).isEqualTo("Registre uma despesa de 85 reais com Uber");
    }

    @Test
    @DisplayName("arquivo ausente e rejeitado antes de chamar o provedor")
    void rejectsNullFile() {
        assertThatThrownBy(() -> service.transcribe(null))
                .isInstanceOf(InvalidAudioException.class)
                .hasMessageContaining("obrigatorio");
    }

    @Test
    @DisplayName("arquivo vazio e rejeitado")
    void rejectsEmptyFile() {
        MockMultipartFile empty = new MockMultipartFile("file", "vazio.mp3", "audio/mpeg", new byte[0]);

        assertThatThrownBy(() -> service.transcribe(empty))
                .isInstanceOf(InvalidAudioException.class);
    }

    @Test
    @DisplayName("formato nao suportado e rejeitado com a lista de formatos validos")
    void rejectsUnsupportedFormat() {
        assertThatThrownBy(() -> service.transcribe(audioFile("nota.txt")))
                .isInstanceOf(InvalidAudioException.class)
                .hasMessageContaining("mp3");
    }

    @Test
    @DisplayName("arquivo acima do limite e rejeitado")
    void rejectsTooLargeFile() {
        AssistantProperties tinyLimit = new AssistantProperties();
        tinyLimit.setMaxAudioSize(DataSize.ofBytes(4));
        TranscriptionService restricted = new TranscriptionService(transcriptionModel, tinyLimit);

        assertThatThrownBy(() -> restricted.transcribe(audioFile("comando.mp3")))
                .isInstanceOf(InvalidAudioException.class)
                .hasMessageContaining("tamanho maximo");
    }

    @Test
    @DisplayName("transcricao vazia vira erro de transcricao, nao uma resposta em branco")
    void emptyTranscriptionFails() {
        given(transcriptionModel.call(any(AudioTranscriptionPrompt.class)))
                .willReturn(new AudioTranscriptionResponse(new AudioTranscription("   ")));

        assertThatThrownBy(() -> service.transcribe(audioFile("comando.mp3")))
                .isInstanceOf(TranscriptionException.class);
    }

    @Test
    @DisplayName("falha do provedor e encapsulada em TranscriptionException")
    void wrapsProviderFailure() {
        given(transcriptionModel.call(any(AudioTranscriptionPrompt.class)))
                .willThrow(new RuntimeException("connection reset"));

        assertThatThrownBy(() -> service.transcribe(audioFile("comando.mp3")))
                .isInstanceOf(TranscriptionException.class)
                .hasRootCauseMessage("connection reset");
    }

    private MockMultipartFile audioFile(String name) {
        return new MockMultipartFile("file", name, "audio/mpeg", "conteudo-de-audio".getBytes());
    }
}
