package com.example.bankcards.repository;

import com.example.bankcards.entity.Transfer;
import com.example.bankcards.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransferRepository extends JpaRepository<Transfer, Long> {

    // Найти переводы, где пользователь является отправителем или получателем
    @Query("SELECT t FROM Transfer t WHERE t.fromCard.owner = :user OR t.toCard.owner = :user")
    Page<Transfer> findByUser(@Param("user") User user, Pageable pageable);

    // Найти переводы по конкретной карте (где карта является отправителем или получателем)
    @Query("SELECT t FROM Transfer t WHERE t.fromCard.id = :cardId OR t.toCard.id = :cardId")
    Page<Transfer> findByCard(@Param("cardId") Long cardId, Pageable pageable);

    // Найти переводы пользователя за период
    @Query("SELECT t FROM Transfer t WHERE (t.fromCard.owner = :user OR t.toCard.owner = :user) " +
            "AND t.transferDate BETWEEN :startDate AND :endDate")
    List<Transfer> findByUserAndPeriod(@Param("user") User user,
                                       @Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);

    // Найти переводы по сумме (больше указанной)
    Page<Transfer> findByAmountGreaterThan(BigDecimal amount, Pageable pageable);
}
