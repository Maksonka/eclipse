package com.example.shadowvibe.Repositories;

import com.example.shadowvibe.Models.UserKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserKeyRepository extends JpaRepository<UserKey, Long> {

    Optional<UserKey> findByUserId(Long userId);
}
