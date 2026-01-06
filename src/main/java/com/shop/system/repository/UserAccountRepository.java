package com.shop.system.repository;

import com.shop.system.domain.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

    Optional<UserAccount> findByLoginAndIsActiveTrue(String login);

    @Query("""
            select ua from UserAccount ua
            join fetch ua.employee e
            left join fetch e.position p
            left join fetch e.department d
            join fetch ua.role r
            where ua.login = :login
              and ua.isActive = true
            """)
    Optional<UserAccount> findActiveWithDetails(String login);
}
