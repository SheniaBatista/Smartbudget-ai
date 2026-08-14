package com.smartbudget.infrastructure.web.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AssistantAudioResponse(String transcription,
                                     String message,
                                     String audioFormat,
                                     String audioBase64) {
    public static AssistantAudioResponse textOnly(String transcription, String message) {
        return new AssistantAudioResponse(transcription, message, null, null);
    }
}
