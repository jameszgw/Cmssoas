package com.codeman.platform.customer.repo;

import com.codeman.platform.customer.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    List<Customer> findAllByOrderByCreatedAtDesc();
    boolean existsByName(String name);
}
