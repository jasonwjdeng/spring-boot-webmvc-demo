package com.example.demo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "agent-service")
public class AgentServiceProperties {

    /**
     * agent-service 基础地址
     */
    private String baseUrl = "http://localhost:9090";

    /**
     * 同步 chat 接口路径
     */
    private String chatPath = "/v1/chat";

    /**
     * 流式 chat 接口路径
     */
    private String chatStreamPath = "/v1/chat/stream";
}
