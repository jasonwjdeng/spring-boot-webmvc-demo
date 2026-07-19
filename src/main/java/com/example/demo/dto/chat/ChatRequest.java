package com.example.demo.dto.chat;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatRequest {

    /**
     * 模型名称
     */
    private String model;

    /**
     * 消息列表
     */
    @NotEmpty(message = "messages不能为空")
    @Valid
    private List<ChatMessage> messages;

    /**
     * 温度参数，控制随机性 (0-2)
     */
    private Double temperature;

    /**
     * 最大生成 token 数
     */
    @JsonProperty("max_tokens")
    private Integer maxTokens;
}
