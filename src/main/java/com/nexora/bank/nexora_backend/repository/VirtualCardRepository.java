package com.nexora.bank.nexora_backend.repository;

import com.nexora.bank.nexora_backend.entity.VirtualCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface VirtualCardRepository extends JpaRepository<VirtualCard, Long> {
    Optional<VirtualCard> findByHolderName(String holderName);
}
