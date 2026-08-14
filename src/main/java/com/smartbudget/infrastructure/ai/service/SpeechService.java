package com.smartbudget.infrastructure.ai.service;

import com.smartbudget.infrastructure.ai.exception.SpeechSynthesisException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.audio.tts.TextToSpeechResponse;
import org.springframework.stereotype.Service;

@Service
public class SpeechService {
    private static final Logger log = LoggerFactory.getLogger(SpeechService.class);

    private final TextToSpeechModel textToSpeechModel;

    public SpeechService(TextToSpeechModel textToSpeechModel) {
        this.textToSpeechModel = textToSpeechModel;
    }

    public byte[] synthesize(String text) {
        if (text == null || text.isBlank()) {
            throw new SpeechSynthesisException("Nao ha texto para converter em audio.", null);
        }

        try {
            TextToSpeechResponse response = textToSpeechModel.call(new TextToSpeechPrompt(text));
            byte[] audio = response.getResult() != null ? response.getResult().getOutput() : null;

            if (audio == null || audio.length == 0) {
                throw new SpeechSynthesisException("O provedor retornou um audio vazio.", null);
            }

            log.info("Speech synthesis completed ({} bytes)", audio.length);
            return audio;

        } catch (SpeechSynthesisException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new SpeechSynthesisException("Falha ao gerar o audio de resposta.", exception);
        }
    }
}
