package com.example.shadowvibe.Repositories;

import com.example.shadowvibe.Models.PushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {

    List<PushSubscription> findByUserUsername(String username);

    Optional<PushSubscription> findByUserUsernameAndEndpoint(String username, String endpoint);

    void deleteByUserUsernameAndEndpoint(String username, String endpoint);
}
