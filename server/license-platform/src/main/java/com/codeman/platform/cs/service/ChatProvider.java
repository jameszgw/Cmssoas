package com.codeman.platform.cs.service;

import java.util.List;
import java.util.function.Consumer;

/**
 * 大模型对话提供方的薄抽象(provider-agnostic)。
 * 默认实现 {@link OpenAiCompatProvider} 走 OpenAI 兼容接口，可对接任意兼容厂商/本地 Ollama。
 * 若将来要接特定厂商私有协议，只需另写一个实现，业务层无感知。
 */
public interface ChatProvider {

    /** 上游是否已正确配置、可调用。 */
    boolean ready();

    /**
     * 流式对话：逐段产生回复文本。
     * @param messages 完整消息序列(含 system/历史/当前用户问题)，role ∈ {system,user,assistant}
     * @param onToken  每收到一段增量文本时回调(用于 SSE 转发给前端)
     * @return 完整回复文本(用于落库)
     */
    String stream(List<Msg> messages, Consumer<String> onToken) throws Exception;

    /** 单条消息。 */
    record Msg(String role, String content) {}
}
