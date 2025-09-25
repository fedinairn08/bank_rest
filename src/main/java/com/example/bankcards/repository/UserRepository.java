package com.example.bankcards.repository;

import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Boolean existsByUsername(String username);

    List<User> findByRolesContaining(Role role);

    // Поиск пользователей по имени
    Page<User> findByUsernameContainingIgnoreCase(String username, Pageable pageable);

    // Найти пользователей с картами (JOIN)
    @Query("SELECT DISTINCT u FROM User u JOIN u.cards c")
    List<User> findUsersWithCards();

    // Получить количество пользователей по роли
    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.name = :roleName")
    long countByRoleName(@Param("roleName") String roleName);

    @Query("SELECT COUNT(c) > 0 FROM Card c WHERE c.owner.id = :userId AND c.balance > 0")
    boolean hasUserCardsWithBalance(@Param("userId") Long userId);
}
