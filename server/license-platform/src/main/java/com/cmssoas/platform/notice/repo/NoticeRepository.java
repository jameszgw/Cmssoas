package com.cmssoas.platform.notice.repo;

import com.cmssoas.platform.notice.domain.Notice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    List<Notice> findAllByOrderByCreatedAtDesc();

    /** 取某类型当前生效(已发布)版本，按版本号最大者。 */
    Optional<Notice> findFirstByTypeAndStatusOrderByVersionDesc(String type, String status);

    List<Notice> findByStatusAndForceAckTrue(String status);

    Optional<Notice> findFirstByTypeOrderByVersionDesc(String type);
}
