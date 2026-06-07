package com.cmssoas.platform.cs.repo;

import com.cmssoas.platform.cs.domain.CsConversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CsConversationRepository extends JpaRepository<CsConversation, Long> {
    List<CsConversation> findAllByOrderByUpdatedAtDesc();
    List<CsConversation> findByUserRefOrderByUpdatedAtDesc(String userRef);
}
