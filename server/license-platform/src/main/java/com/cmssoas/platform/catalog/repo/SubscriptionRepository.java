package com.cmssoas.platform.catalog.repo;

import com.cmssoas.platform.catalog.domain.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    List<Subscription> findAllByOrderByCreatedAtDesc();
}
