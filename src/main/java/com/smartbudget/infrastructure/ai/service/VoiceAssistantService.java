package com.smartbudget.infrastructure.ai.service;

import com.smartbudget.infrastructure.ai.config.AssistantProperties;
import com.smartbudget.infrastructure.web.response.AssistantAudioResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;

@Service
public class VoiceAssistantService {
    private static final String AUDIO_FORMAT = "mp3";

    private static final Logger log = LoggerFactory.getLogger(VoiceAssistantService.class);

    private final TranscriptionService transcriptionService;
    private final AssistantService assistantService;
    private final SpeechService speechService;
    private final AssistantProperties properties;

    public VoiceAssistantService(TranscriptionService transcriptionService,
                                 AssistantService assistantService,
                                 SpeechService speechService,
                                 AssistantProperties properties) {
        this.transcriptionService = transcriptionService;
        this.assistantService = assistantService;
        this.speechService = speechService;
        this.properties = properties;
    }

    public AssistantAudioResponse handle(MultipartFile audio, boolean speak) {
        String transcription = transcriptionService.transcribe(audio);
        String reply = assistantService.reply(transcription);

        if (!speak || !properties.isSpeechEnabled()) {
            log.info("Voice request processed (text only)");
            return AssistantAudioResponse.textOnly(transcription, reply);
        }

        byte[] speech = speechService.synthesize(reply);
        log.info("Voice request processed (with speech)");

        return new AssistantAudioResponse(
                transcription,
                reply,
                AUDIO_FORMAT,
                Base64.getEncoder().encodeToString(speech));
    }

    public byte[] handleAsSpeech(MultipartFile audio) {
        String transcription = transcriptionService.transcribe(audio);
        String reply = assistantService.reply(transcription);
        byte[] speech = speechService.synthesize(reply);

        log.info("Voice request processed (audio/mpeg)");
        return speech;
    }
}
