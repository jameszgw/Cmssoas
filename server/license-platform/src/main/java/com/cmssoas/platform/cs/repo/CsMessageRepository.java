package com.cmssoas.platform.cs.repo;

import com.cmssoas.platform.cs.domain.CsMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CsMessageRepository extends JpaRepository<CsMessage, Long> {
    List<CsMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);
}
