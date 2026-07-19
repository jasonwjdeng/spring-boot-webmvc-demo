package com.example.demo.controller;

import com.example.demo.adapter.AgentAdapter;
import com.example.demo.dto.chat.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AgentController.class)
class AgentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AgentAdapter agentAdapter;

    @Test
    @DisplayName("同步 chat - 成功")
    void chat_Success() throws Exception {
        ChatResponse mockResponse = ChatResponse.builder()
                .id("chatcmpl-123")
                .object("chat.completion")
                .created(1700000000L)
                .model("gpt-4")
                .choices(List.of(ChatResponse.Choice.builder()
                        .index(0)
                        .message(ChatMessage.builder().role("assistant").content("你好！").build())
                        .finishReason("stop")
                        .build()))
                .usage(ChatResponse.Usage.builder()
                        .promptTokens(10)
                        .completionTokens(5)
                        .totalTokens(15)
                        .build())
                .build();

        when(agentAdapter.chat(any(ChatRequest.class))).thenReturn(mockResponse);

        ChatRequest request = ChatRequest.builder()
                .model("gpt-4")
                .messages(List.of(ChatMessage.builder().role("user").content("你好").build()))
                .build();

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("chatcmpl-123"))
                .andExpect(jsonPath("$.data.choices[0].message.content").value("你好！"))
                .andExpect(jsonPath("$.data.choices[0].finish_reason").value("stop"));
    }

    @Test
    @DisplayName("同步 chat - messages为空时校验失败")
    void chat_EmptyMessages_ValidationError() throws Exception {
        ChatRequest request = ChatRequest.builder()
                .model("gpt-4")
                .messages(List.of())
                .build();

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("流式 chat/stream - 返回 SSE")
    void chatStream_Success() throws Exception {
        SseEmitter emitter = new SseEmitter();

        when(agentAdapter.chatStream(any(ChatRequest.class))).thenReturn(emitter);

        // 模拟异步发送数据
        new Thread(() -> {
            try {
                Thread.sleep(100);
                ChatCompletionChunk chunk = ChatCompletionChunk.builder()
                        .id("chatcmpl-456")
                        .object("chat.completion.chunk")
                        .created(1700000000L)
                        .model("gpt-4")
                        .choices(List.of(ChatCompletionChunk.ChunkChoice.builder()
                                .index(0)
                                .delta(ChatMessage.builder().role("assistant").content("你").build())
                                .build()))
                        .build();
                emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(chunk)));
                emitter.send(SseEmitter.event().data("[DONE]"));
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        }).start();

        ChatRequest request = ChatRequest.builder()
                .model("gpt-4")
                .messages(List.of(ChatMessage.builder().role("user").content("你好").build()))
                .build();

        mockMvc.perform(post("/api/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted());
    }

    @Test
    @DisplayName("流式 chat/stream - messages为空时校验失败")
    void chatStream_EmptyMessages_ValidationError() throws Exception {
        ChatRequest request = ChatRequest.builder()
                .model("gpt-4")
                .messages(List.of())
                .build();

        mockMvc.perform(post("/api/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }
}
