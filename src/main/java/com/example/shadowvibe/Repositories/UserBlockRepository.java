package com.example.shadowvibe.Repositories;

import com.example.shadowvibe.Models.UserBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {

    boolean existsByBlockerUsernameAndBlockedUsername(String blockerUsername, String blockedUsername);

    Optional<UserBlock> findByBlockerUsernameAndBlockedUsername(String blockerUsername, String blockedUsername);

    List<UserBlock> findByBlockerUsernameOrderByCreatedAtDesc(String blockerUsername);

    void deleteByBlockerUsernameAndBlockedUsername(String blockerUsername, String blockedUsername);
}
