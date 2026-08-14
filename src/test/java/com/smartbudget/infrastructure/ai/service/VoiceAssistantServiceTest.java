package com.smartbudget.infrastructure.ai.service;

import com.smartbudget.infrastructure.ai.config.AssistantProperties;
import com.smartbudget.infrastructure.ai.exception.TranscriptionException;
import com.smartbudget.infrastructure.web.response.AssistantAudioResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DisplayName("VoiceAssistantService - orquestracao do fluxo de voz")
class VoiceAssistantServiceTest {
    private static final MultipartFile AUDIO =
            new MockMultipartFile("file", "comando.mp3", "audio/mpeg", "bytes".getBytes());

    private TranscriptionService transcriptionService;
    private AssistantService assistantService;
    private SpeechService speechService;
    private VoiceAssistantService service;

    @BeforeEach
    void setUp() {
        transcriptionService = mock(TranscriptionService.class);
        assistantService = mock(AssistantService.class);
        speechService = mock(SpeechService.class);
        service = new VoiceAssistantService(
                transcriptionService, assistantService, speechService, new AssistantProperties());
    }

    @Test
    @DisplayName("encadeia transcricao, assistente e sintese, devolvendo audio em Base64")
    void runsFullPipeline() {
        byte[] audioBytes = {10, 20, 30};
        given(transcriptionService.transcribe(any())).willReturn("quanto eu gastei hoje");
        given(assistantService.reply("quanto eu gastei hoje")).willReturn("Hoje você gastou R$ 60,50.");
        given(speechService.synthesize(anyString())).willReturn(audioBytes);

        AssistantAudioResponse response = service.handle(AUDIO, true);

        assertThat(response.transcription()).isEqualTo("quanto eu gastei hoje");
        assertThat(response.message()).isEqualTo("Hoje você gastou R$ 60,50.");
        assertThat(response.audioFormat()).isEqualTo("mp3");
        assertThat(response.audioBase64()).isEqualTo(Base64.getEncoder().encodeToString(audioBytes));
    }

    @Test
    @DisplayName("com speak desligado, poupa a chamada de sintese de voz")
    void skipsSpeechWhenDisabled() {
        given(transcriptionService.transcribe(any())).willReturn("qual e o meu saldo");
        given(assistantService.reply(anyString())).willReturn("Seu saldo e de R$ 3.860,00.");

        AssistantAudioResponse response = service.handle(AUDIO, false);

        assertThat(response.audioBase64()).isNull();
        assertThat(response.audioFormat()).isNull();
        assertThat(response.message()).isEqualTo("Seu saldo e de R$ 3.860,00.");
        verify(speechService, never()).synthesize(anyString());
    }

    @Test
    @DisplayName("configuracao global desliga a sintese mesmo com speak ligado")
    void globalSwitchDisablesSpeech() {
        AssistantProperties silent = new AssistantProperties();
        silent.setSpeechEnabled(false);
        VoiceAssistantService quiet = new VoiceAssistantService(
                transcriptionService, assistantService, speechService, silent);

        given(transcriptionService.transcribe(any())).willReturn("resumo do mes");
        given(assistantService.reply(anyString())).willReturn("Resumo gerado.");

        assertThat(quiet.handle(AUDIO, true).audioBase64()).isNull();
        verify(speechService, never()).synthesize(anyString());
    }

    @Test
    @DisplayName("falha na transcricao interrompe o fluxo antes de consultar o assistente")
    void stopsWhenTranscriptionFails() {
        given(transcriptionService.transcribe(any()))
                .willThrow(new TranscriptionException("audio ilegivel"));

        assertThatThrownBy(() -> service.handle(AUDIO, true))
                .isInstanceOf(TranscriptionException.class);

        verify(assistantService, never()).reply(anyString());
        verify(speechService, never()).synthesize(anyString());
    }
}
