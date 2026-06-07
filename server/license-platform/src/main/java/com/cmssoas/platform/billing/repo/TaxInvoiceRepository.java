package com.cmssoas.platform.billing.repo;

import com.cmssoas.platform.billing.domain.TaxInvoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaxInvoiceRepository extends JpaRepository<TaxInvoice, Long> {
    List<TaxInvoice> findAllByOrderByCreatedAtDesc();
    List<TaxInvoice> findByInvoiceId(Long invoiceId);
}
