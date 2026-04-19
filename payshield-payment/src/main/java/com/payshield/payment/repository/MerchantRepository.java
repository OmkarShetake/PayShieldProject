package com.payshield.payment.repository;

import com.payshield.payment.entity.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface MerchantRepository extends JpaRepository<Merchant, UUID> {
    Optional<Merchant> findByApiKey(String apiKey);
    boolean existsByEmail(String email);
}
