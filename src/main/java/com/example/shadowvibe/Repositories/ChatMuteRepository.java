package com.example.shadowvibe.Repositories;

import com.example.shadowvibe.Models.ChatMute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMuteRepository extends JpaRepository<ChatMute, Long> {

    Optional<ChatMute> findByUserUsernameAndPartnerUsername(String userUsername, String partnerUsername);

    boolean existsByUserUsernameAndPartnerUsernameAndMutedTrue(String userUsername, String partnerUsername);

    @Query("select m.partner.username from ChatMute m where m.user.username = :username and m.muted = true")
    List<String> findMutedPartnerUsernamesByUserUsername(@Param("username") String username);
}
