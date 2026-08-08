package com.example.shadowvibe.Repositories;

import com.example.shadowvibe.Models.WatchRoom;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WatchRoomRepository extends JpaRepository<WatchRoom, Long> {

    Optional<WatchRoom> findByRoomCode(String roomCode);

    List<WatchRoom> findByOrderByCreatedAtDesc();

    @Query("SELECT r FROM WatchRoom r WHERE r.visibility = com.example.shadowvibe.enums.RoomVisibility.PUBLIC ORDER BY r.updatedAt DESC, r.createdAt DESC")
    List<WatchRoom> findPublicByOrderByUpdatedAtDesc();

    @Query("SELECT DISTINCT r FROM WatchRoom r JOIN r.members m WHERE m.username = :username ORDER BY r.createdAt DESC")
    List<WatchRoom> findAllByMemberUsername(@Param("username") String username);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM WatchRoom r WHERE r.id = :id")
    Optional<WatchRoom> lockRoomById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM WatchRoom r WHERE r.roomCode = :roomCode")
    Optional<WatchRoom> lockRoomByCode(@Param("roomCode") String roomCode);

    @Query("SELECT DISTINCT r FROM WatchRoom r LEFT JOIN FETCH r.members WHERE r.id = :id")
    Optional<WatchRoom> findWithMembersById(@Param("id") Long id);
}
