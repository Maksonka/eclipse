package com.example.shadowvibe.Repositories;

import com.example.shadowvibe.Models.ScheduledMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ScheduledMessageRepository extends JpaRepository<ScheduledMessage, Long> {

    List<ScheduledMessage> findBySenderUsernameAndStatusOrderByScheduleAtAsc(String username, String status);

    List<ScheduledMessage> findByStatusAndScheduleAtLessThanEqual(String status, LocalDateTime dueAt);
}
