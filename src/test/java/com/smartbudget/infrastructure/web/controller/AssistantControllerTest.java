package com.smartbudget.infrastructure.web.controller;

import com.smartbudget.infrastructure.ai.exception.AiAssistantException;
import com.smartbudget.infrastructure.ai.exception.InvalidAudioException;
import com.smartbudget.infrastructure.ai.service.AssistantService;
import com.smartbudget.infrastructure.ai.service.VoiceAssistantService;
import com.smartbudget.infrastructure.web.response.AssistantAudioResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AssistantController.class)
@DisplayName("AssistantController")
class AssistantControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AssistantService assistantService;

    @MockitoBean
    private VoiceAssistantService voiceAssistantService;

    @Test
    @DisplayName("POST /message devolve a resposta do assistente")
    void repliesToTextMessage() throws Exception {
        given(assistantService.reply(anyString()))
                .willReturn("Despesa de R$ 85,00 com Uber registrada com sucesso.");

        mockMvc.perform(post("/api/v1/assistant/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"Registre uma despesa de 85 reais com Uber"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Despesa de R$ 85,00 com Uber registrada com sucesso."));
    }

    @Test
    @DisplayName("POST /message com mensagem vazia devolve 400")
    void rejectsBlankMessage() throws Exception {
        mockMvc.perform(post("/api/v1/assistant/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"   "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("falha do provedor de IA vira 502 sem vazar detalhe interno")
    void mapsAiFailureToBadGateway() throws Exception {
        given(assistantService.reply(anyString()))
                .willThrow(new AiAssistantException("401 Unauthorized do provedor com detalhes internos"));

        mockMvc.perform(post("/api/v1/assistant/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"Qual e o meu saldo?"}
                                """))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("AI_SERVICE_ERROR"))
                .andExpect(jsonPath("$.message").value("O assistente esta indisponivel no momento. Tente novamente em instantes."));
    }

    @Test
    @DisplayName("POST /audio devolve transcricao, resposta e audio em Base64")
    void handlesAudio() throws Exception {
        given(voiceAssistantService.handle(any(), anyBoolean())).willReturn(new AssistantAudioResponse(
                "quanto eu gastei hoje",
                "Hoje você gastou R$ 60,50.",
                "mp3",
                "QUJD"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "comando.mp3", "audio/mpeg", "conteudo-fake".getBytes());

        mockMvc.perform(multipart("/api/v1/assistant/audio").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transcription").value("quanto eu gastei hoje"))
                .andExpect(jsonPath("$.message").value("Hoje você gastou R$ 60,50."))
                .andExpect(jsonPath("$.audioFormat").value("mp3"))
                .andExpect(jsonPath("$.audioBase64").value("QUJD"));
    }

    @Test
    @DisplayName("com Accept audio/mpeg, o mesmo endpoint devolve o MP3 puro")
    void handlesAudioAsMp3() throws Exception {
        byte[] mp3 = {1, 2, 3, 4};
        given(voiceAssistantService.handleAsSpeech(any())).willReturn(mp3);

        MockMultipartFile file = new MockMultipartFile(
                "file", "comando.mp3", "audio/mpeg", "conteudo-fake".getBytes());

        mockMvc.perform(multipart("/api/v1/assistant/audio").file(file).accept("audio/mpeg"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("audio/mpeg"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"resposta.mp3\""))
                .andExpect(content().bytes(mp3));
    }

    @Test
    @DisplayName("audio em formato invalido devolve 400")
    void rejectsInvalidAudio() throws Exception {
        given(voiceAssistantService.handle(any(), anyBoolean()))
                .willThrow(new InvalidAudioException("Formato de audio nao suportado."));

        MockMultipartFile file = new MockMultipartFile(
                "file", "comando.txt", "text/plain", "nao e audio".getBytes());

        mockMvc.perform(multipart("/api/v1/assistant/audio").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_AUDIO"));
    }

    @Test
    @DisplayName("requisicao de audio sem arquivo devolve 400")
    void rejectsMissingFile() throws Exception {
        mockMvc.perform(multipart("/api/v1/assistant/audio"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MISSING_FILE"));
    }

    @Test
    @DisplayName("o caminho audio/mpeg nao depende do campo Base64, evitando NPE quando a voz esta desligada")
    void audioAsMp3DoesNotDependOnBase64Field() throws Exception {
        given(voiceAssistantService.handleAsSpeech(any())).willReturn(new byte[]{9, 9, 9});

        MockMultipartFile file = new MockMultipartFile(
                "file", "comando.mp3", "audio/mpeg", "conteudo-fake".getBytes());

        mockMvc.perform(multipart("/api/v1/assistant/audio").file(file).accept("audio/mpeg"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(new byte[]{9, 9, 9}));

        verify(voiceAssistantService, never()).handle(any(), anyBoolean());
    }

    @Test
    @DisplayName("metodo HTTP errado devolve 405, nao 500")
    void wrongMethodReturns405() throws Exception {
        mockMvc.perform(get("/api/v1/assistant/message"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.error").value("METHOD_NOT_ALLOWED"));
    }
}
