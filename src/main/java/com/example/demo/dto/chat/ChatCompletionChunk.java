package com.example.demo.dto.chat;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * SSE 流式响应的单个 chunk（类似 OpenAI 格式）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatCompletionChunk {

    private String id;

    private String object;

    private Long created;

    private String model;

    private List<ChunkChoice> choices;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChunkChoice {

        private Integer index;

        /**
         * 流式模式下使用 delta 而非 message
         */
        private ChatMessage delta;

        @JsonProperty("finish_reason")
        private String finishReason;
    }
}
