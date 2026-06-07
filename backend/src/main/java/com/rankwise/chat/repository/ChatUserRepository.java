package com.rankwise.chat.repository;

import com.rankwise.chat.entity.ChatUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatUserRepository extends JpaRepository<ChatUser, Long> {
    Optional<ChatUser> findByVisitorToken(String visitorToken);
}
