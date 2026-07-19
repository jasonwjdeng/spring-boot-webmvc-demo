package com.example.demo.adapter;

import com.example.demo.config.AgentServiceProperties;
import com.example.demo.dto.chat.ChatCompletionChunk;
import com.example.demo.dto.chat.ChatRequest;
import com.example.demo.dto.chat.ChatResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Agent 服务适配器，负责调用 agent-service 的 chat 和 chat/stream 接口
 */
@Slf4j
@Component
public class AgentAdapter {

    private static final String SSE_DATA_PREFIX = "data: ";
    private static final String SSE_DONE_SIGNAL = "[DONE]";

    private final RestClient agentRestClient;
    private final AgentServiceProperties properties;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public AgentAdapter(RestClient agentRestClient,
                        AgentServiceProperties properties,
                        ObjectMapper objectMapper) {
        this.agentRestClient = agentRestClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /**
     * 同步调用 agent-service 的 chat 接口
     */
    public ChatResponse chat(ChatRequest request) {
        return agentRestClient.post()
                .uri(properties.getChatPath())
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(ChatResponse.class);
    }

    /**
     * 流式调用 agent-service 的 chat/stream 接口，通过 SSE 推送给客户端
     */
    public SseEmitter chatStream(ChatRequest request) {
        SseEmitter emitter = new SseEmitter(120_000L);

        executor.execute(() -> {
            try {
                InputStream inputStream = agentRestClient.post()
                        .uri(properties.getChatStreamPath())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .body(request)
                        .retrieve()
                        .body(InputStream.class);

                if (inputStream == null) {
                    emitter.completeWithError(new RuntimeException("Empty response from agent-service"));
                    return;
                }

                parseSseStream(inputStream, emitter);

            } catch (Exception e) {
                log.error("调用 agent-service stream 失败", e);
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    /**
     * 解析 SSE 流并逐条转发给客户端
     */
    private void parseSseStream(InputStream inputStream, SseEmitter emitter) throws IOException {
        parseSseStream(inputStream, data -> {
            try {
                emitter.send(SseEmitter.event().data(data, MediaType.APPLICATION_JSON));
            } catch (IOException e) {
                throw new RuntimeException("Failed to send SSE event", e);
            }
        }, emitter::complete, emitter::completeWithError);
    }

    /**
     * 解析 SSE 流的核心逻辑（可独立测试）
     *
     * @param inputStream SSE 输入流
     * @param onData      每收到一条数据的回调
     * @param onComplete  流结束回调
     * @param onError     错误回调
     */
    void parseSseStream(InputStream inputStream, Consumer<String> onData,
                        Runnable onComplete, Consumer<Throwable> onError) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    continue;
                }

                if (!line.startsWith(SSE_DATA_PREFIX)) {
                    continue;
                }

                String data = line.substring(SSE_DATA_PREFIX.length()).trim();

                // 流结束信号
                if (SSE_DONE_SIGNAL.equals(data)) {
                    onData.accept(SSE_DONE_SIGNAL);
                    onComplete.run();
                    return;
                }

                // 解析 chunk 并转发
                ChatCompletionChunk chunk = objectMapper.readValue(data, ChatCompletionChunk.class);
                onData.accept(objectMapper.writeValueAsString(chunk));
            }

            // 流正常结束但未收到 [DONE]
            onComplete.run();

        } catch (Exception e) {
            log.error("读取 SSE 流异常", e);
            onError.accept(e);
        }
    }
}
