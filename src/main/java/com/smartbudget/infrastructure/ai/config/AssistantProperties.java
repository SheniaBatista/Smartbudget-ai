package com.smartbudget.infrastructure.ai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "smartbudget.assistant")
public class AssistantProperties {
    private DataSize maxAudioSize = DataSize.ofMegabytes(20);

    private List<String> allowedAudioExtensions =
            List.of("mp3", "mp4", "mpeg", "mpga", "m4a", "wav", "webm", "ogg", "flac");

    private boolean speechEnabled = true;
}
