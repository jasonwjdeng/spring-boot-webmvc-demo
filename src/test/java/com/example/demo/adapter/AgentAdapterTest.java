package com.example.demo.adapter;

import com.example.demo.config.AgentServiceProperties;
import com.example.demo.dto.chat.ChatMessage;
import com.example.demo.dto.chat.ChatRequest;
import com.example.demo.dto.chat.ChatResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentAdapterTest {

    @Mock
    private RestClient agentRestClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private AgentServiceProperties properties;
    private ObjectMapper objectMapper;
    private AgentAdapter agentAdapter;

    @BeforeEach
    void setUp() {
        properties = new AgentServiceProperties();
        properties.setBaseUrl("http://localhost:9090");
        properties.setChatPath("/v1/chat");
        properties.setChatStreamPath("/v1/chat/stream");
        objectMapper = new ObjectMapper();
        agentAdapter = new AgentAdapter(agentRestClient, properties, objectMapper);
    }

    @Test
    @DisplayName("同步 chat - 正确调用并返回响应")
    void chat_Success() {
        ChatResponse expectedResponse = ChatResponse.builder()
                .id("chatcmpl-123")
                .object("chat.completion")
                .model("gpt-4")
                .choices(List.of(ChatResponse.Choice.builder()
                        .index(0)
                        .message(ChatMessage.builder().role("assistant").content("Hello!").build())
                        .finishReason("stop")
                        .build()))
                .build();

        // Mock RestClient 链式调用
        when(agentRestClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/v1/chat")).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(ChatRequest.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(ChatResponse.class)).thenReturn(expectedResponse);

        ChatRequest request = ChatRequest.builder()
                .model("gpt-4")
                .messages(List.of(ChatMessage.builder().role("user").content("Hi").build()))
                .build();

        ChatResponse result = agentAdapter.chat(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("chatcmpl-123");
        assertThat(result.getChoices().get(0).getMessage().getContent()).isEqualTo("Hello!");

        verify(agentRestClient).post();
        verify(requestBodyUriSpec).uri("/v1/chat");
    }

    @Test
    @DisplayName("流式 parseSseStream - 正确解析 SSE 数据")
    void parseSseStream_ParsesCorrectly() {
        // 构造模拟的 SSE 响应
        String sseData = """
                data: {"id":"chatcmpl-1","object":"chat.completion.chunk","created":1700000000,"model":"gpt-4","choices":[{"index":0,"delta":{"role":"assistant","content":"你"},"finish_reason":null}]}
                
                data: {"id":"chatcmpl-1","object":"chat.completion.chunk","created":1700000000,"model":"gpt-4","choices":[{"index":0,"delta":{"content":"好"},"finish_reason":null}]}
                
                data: {"id":"chatcmpl-1","object":"chat.completion.chunk","created":1700000000,"model":"gpt-4","choices":[{"index":0,"delta":{},"finish_reason":"stop"}]}
                
                data: [DONE]
                
                """;

        InputStream inputStream = new ByteArrayInputStream(sseData.getBytes(StandardCharsets.UTF_8));

        List<String> receivedData = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        agentAdapter.parseSseStream(inputStream, receivedData::add, () -> completed.set(true), errorRef::set);

        assertThat(errorRef.get()).isNull();
        assertThat(completed.get()).isTrue();
        // 3 个 chunk + 1 个 [DONE]
        assertThat(receivedData).hasSize(4);
        assertThat(receivedData.get(0)).contains("\"content\":\"你\"");
        assertThat(receivedData.get(1)).contains("\"content\":\"好\"");
        assertThat(receivedData.get(2)).contains("\"finish_reason\":\"stop\"");
        assertThat(receivedData.get(3)).isEqualTo("[DONE]");
    }

    @Test
    @DisplayName("流式 parseSseStream - 无 [DONE] 信号时正常结束")
    void parseSseStream_CompletesWithoutDone() {
        String sseData = """
                data: {"id":"chatcmpl-1","object":"chat.completion.chunk","created":1700000000,"model":"gpt-4","choices":[{"index":0,"delta":{"content":"hi"},"finish_reason":null}]}
                
                """;

        InputStream inputStream = new ByteArrayInputStream(sseData.getBytes(StandardCharsets.UTF_8));

        List<String> receivedData = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);

        agentAdapter.parseSseStream(inputStream, receivedData::add, () -> completed.set(true), e -> {});

        assertThat(completed.get()).isTrue();
        assertThat(receivedData).hasSize(1);
    }

    @Test
    @DisplayName("流式 parseSseStream - 无效 JSON 触发错误回调")
    void parseSseStream_InvalidJson_TriggersError() {
        String sseData = "data: {invalid-json}\n\n";

        InputStream inputStream = new ByteArrayInputStream(sseData.getBytes(StandardCharsets.UTF_8));

        AtomicBoolean completed = new AtomicBoolean(false);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        agentAdapter.parseSseStream(inputStream, data -> {}, () -> completed.set(true), errorRef::set);

        assertThat(completed.get()).isFalse();
        assertThat(errorRef.get()).isNotNull();
    }
}
