package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {

    Page<Card> findAll(Pageable pageable);

    Page<Card> findByOwner(User user, Pageable pageable);

    boolean existsByNumber(String encryptedCardNumber);
}
