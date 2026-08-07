package com.example.testtitle.Repositories;

import com.example.testtitle.Models.WatchRoomPlaylistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WatchRoomPlaylistItemRepository extends JpaRepository<WatchRoomPlaylistItem, Long> {

    @Query("SELECT p FROM WatchRoomPlaylistItem p WHERE p.room.id = :roomId ORDER BY p.position ASC, p.id ASC")
    List<WatchRoomPlaylistItem> findAllByRoomIdOrdered(@Param("roomId") Long roomId);

    void deleteByRoomId(Long roomId);
}
