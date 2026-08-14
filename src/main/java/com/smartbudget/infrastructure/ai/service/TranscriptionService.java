package com.smartbudget.infrastructure.ai.service;

import com.smartbudget.infrastructure.ai.config.AssistantProperties;
import com.smartbudget.infrastructure.ai.exception.InvalidAudioException;
import com.smartbudget.infrastructure.ai.exception.TranscriptionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;

@Service
public class TranscriptionService {
    private static final Logger log = LoggerFactory.getLogger(TranscriptionService.class);

    private final TranscriptionModel transcriptionModel;
    private final AssistantProperties properties;

    public TranscriptionService(TranscriptionModel transcriptionModel, AssistantProperties properties) {
        this.transcriptionModel = transcriptionModel;
        this.properties = properties;
    }

    public String transcribe(MultipartFile file) {
        validate(file);

        try {
            AudioTranscriptionResponse response =
                    transcriptionModel.call(new AudioTranscriptionPrompt(toResource(file)));

            String text = response.getResult() != null ? response.getResult().getOutput() : null;
            if (text == null || text.isBlank()) {
                throw new TranscriptionException("A transcricao retornou vazia.");
            }

            log.info("Audio transcription completed ({} caracteres)", text.length());
            return text.trim();

        } catch (TranscriptionException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new TranscriptionException("Falha ao transcrever o audio enviado.", exception);
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidAudioException("O arquivo de audio e obrigatorio e nao pode estar vazio.");
        }
        if (file.getSize() > properties.getMaxAudioSize().toBytes()) {
            throw new InvalidAudioException(
                    "O audio excede o tamanho maximo de " + properties.getMaxAudioSize().toMegabytes() + " MB.");
        }

        String extension = extensionOf(file.getOriginalFilename());
        if (!properties.getAllowedAudioExtensions().contains(extension)) {
            throw new InvalidAudioException("Formato de audio nao suportado. Use um destes: "
                    + String.join(", ", properties.getAllowedAudioExtensions()) + ".");
        }
    }

    private Resource toResource(MultipartFile file) throws IOException {
        String filename = "audio." + extensionOf(file.getOriginalFilename());
        return new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
    }

    private String extensionOf(String filename) {
        if (filename == null) {
            return "";
        }
        int separator = filename.lastIndexOf('.');
        return separator < 0 ? "" : filename.substring(separator + 1).toLowerCase(Locale.ROOT);
    }
}
