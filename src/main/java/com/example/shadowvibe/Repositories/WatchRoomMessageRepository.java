package com.example.shadowvibe.Repositories;

import com.example.shadowvibe.Models.WatchRoomMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WatchRoomMessageRepository extends JpaRepository<WatchRoomMessage, Long> {

    List<WatchRoomMessage> findAllByRoomIdOrderByTimestampAsc(Long roomId);

    void deleteByRoomId(Long roomId);
}
