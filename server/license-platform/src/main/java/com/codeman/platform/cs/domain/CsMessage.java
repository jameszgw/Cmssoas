package com.codeman.platform.cs.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** 客服会话中的单条消息(user/assistant/system)。 */
@Entity
@Table(name = "cs_message")
public class CsMessage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "conversation_id", nullable = false) private Long conversationId;
    @Column(nullable = false, length = 16) private String role;     // user/assistant/system
    @Column(nullable = false, columnDefinition = "text") private String content;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;

    public CsMessage() {}
    public CsMessage(Long conversationId, String role, String content) {
        this.conversationId = conversationId;
        this.role = role;
        this.content = content;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long v) { this.conversationId = v; }
    public String getRole() { return role; }
    public void setRole(String v) { this.role = v; }
    public String getContent() { return content; }
    public void setContent(String v) { this.content = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
}
