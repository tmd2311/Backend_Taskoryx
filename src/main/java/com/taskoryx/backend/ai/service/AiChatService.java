package com.taskoryx.backend.ai.service;

/**
 * Abstraction over third-party AI chat/completion APIs.
 * Swap implementations (Gemini, OpenAI, Groq...) without touching business logic.
 */
public interface AiChatService {

    /**
     * Gửi một lượt hỏi-đáp đơn giản tới AI.
     *
     * @param systemPrompt hướng dẫn system role cho AI
     * @param userMessage  câu hỏi hoặc yêu cầu của user
     * @return raw text response từ AI (thường là JSON)
     */
    String chat(String systemPrompt, String userMessage);

    /** Tên model đang dùng — dùng để hiển thị trong response metadata. */
    String getModelName();
}
