package com.example.testtitle.Repositories;

import com.example.testtitle.Models.GroupMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GroupMessageRepository extends JpaRepository<GroupMessage, Long> {

    @Query("SELECT gm FROM GroupMessage gm WHERE gm.group.id = :groupId ORDER BY gm.timestamp ASC")
    List<GroupMessage> findByGroupIdOrderByTimestampAsc(@Param("groupId") Long groupId);

    @Query("SELECT gm FROM GroupMessage gm WHERE gm.group.id IN :groupIds ORDER BY gm.timestamp DESC")
    List<GroupMessage> findRecentByGroupIds(@Param("groupIds") List<Long> groupIds);

    @Query("SELECT COUNT(gm) FROM GroupMessage gm WHERE gm.group.id = :groupId AND gm.sender.username <> :username " +
           "AND (:lastReadAt IS NULL OR gm.timestamp > :lastReadAt)")
    long countUnreadForMember(@Param("groupId") Long groupId,
                              @Param("username") String username,
                              @Param("lastReadAt") LocalDateTime lastReadAt);
}
