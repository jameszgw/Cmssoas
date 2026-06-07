package com.cmssoas.platform.billing.repo;

import com.cmssoas.platform.billing.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findAllByOrderByCreatedAtDesc();
    Optional<Payment> findByPaymentNo(String paymentNo);
    List<Payment> findByInvoiceIdOrderByCreatedAtDesc(Long invoiceId);
}
