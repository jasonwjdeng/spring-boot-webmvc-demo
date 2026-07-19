package com.example.demo.controller;

import com.example.demo.adapter.AgentAdapter;
import com.example.demo.dto.Result;
import com.example.demo.dto.chat.ChatRequest;
import com.example.demo.dto.chat.ChatResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AgentController {

    private final AgentAdapter agentAdapter;

    /**
     * 同步 chat 接口
     */
    @PostMapping("/chat")
    public Result<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        ChatResponse response = agentAdapter.chat(request);
        return Result.success(response);
    }

    /**
     * 流式 chat 接口（SSE）
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@Valid @RequestBody ChatRequest request) {
        return agentAdapter.chatStream(request);
    }
}
