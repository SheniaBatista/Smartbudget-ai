package com.smartbudget.infrastructure.web.controller;

import com.smartbudget.infrastructure.ai.service.AssistantService;
import com.smartbudget.infrastructure.ai.service.VoiceAssistantService;
import com.smartbudget.infrastructure.web.request.AssistantMessageRequest;
import com.smartbudget.infrastructure.web.response.AssistantAudioResponse;
import com.smartbudget.infrastructure.web.response.AssistantMessageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/assistant")
public class AssistantController {
    private final AssistantService assistantService;
    private final VoiceAssistantService voiceAssistantService;

    public AssistantController(AssistantService assistantService,
                               VoiceAssistantService voiceAssistantService) {
        this.assistantService = assistantService;
        this.voiceAssistantService = voiceAssistantService;
    }

    @PostMapping("/message")
    public AssistantMessageResponse message(@Valid @RequestBody AssistantMessageRequest request) {
        return new AssistantMessageResponse(assistantService.reply(request.message()));
    }

    @PostMapping(value = "/audio",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public AssistantAudioResponse audio(@RequestPart("file") MultipartFile file,
                                        @RequestParam(defaultValue = "true") boolean speak) {
        return voiceAssistantService.handle(file, speak);
    }

    @PostMapping(value = "/audio",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = "audio/mpeg")
    public ResponseEntity<byte[]> audioAsMp3(@RequestPart("file") MultipartFile file) {
        byte[] mp3 = voiceAssistantService.handleAsSpeech(file);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"resposta.mp3\"")
                .body(mp3);
    }
}
