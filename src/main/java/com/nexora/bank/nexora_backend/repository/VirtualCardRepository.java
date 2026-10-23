package com.nexora.bank.nexora_backend.repository;

import com.nexora.bank.nexora_backend.entity.VirtualCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface VirtualCardRepository extends JpaRepository<VirtualCard, Long> {

    /**
     * Cards are addressed by their owning user, never by holder name. A name is
     * not an identity — it is guessable and not unique — so looking a card up
     * by one hands card data to anyone able to guess a name.
     */
    Optional<VirtualCard> findByUserId(Long userId);
}
