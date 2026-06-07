package com.cmssoas.platform.notice.repo;

import com.cmssoas.platform.notice.domain.UserConsent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserConsentRepository extends JpaRepository<UserConsent, Long> {
    List<UserConsent> findAllByOrderByCreatedAtDesc();

    List<UserConsent> findBySubjectOrderByCreatedAtDesc(String subject);

    /** 主体对某须知的最新一条同意记录（用于判断当前是否已同意/已撤回）。 */
    UserConsent findFirstBySubjectAndNoticeIdOrderByCreatedAtDesc(String subject, Long noticeId);
}
