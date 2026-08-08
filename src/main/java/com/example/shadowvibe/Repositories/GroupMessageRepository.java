package com.example.shadowvibe.Repositories;

import com.example.shadowvibe.Models.GroupMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupMessageRepository extends JpaRepository<GroupMessage, Long> {

    @Query("SELECT gm FROM GroupMessage gm WHERE gm.group.id = :groupId ORDER BY gm.timestamp ASC")
    List<GroupMessage> findByGroupIdOrderByTimestampAsc(@Param("groupId") Long groupId);

    @Query("SELECT gm FROM GroupMessage gm WHERE gm.group.id IN :groupIds ORDER BY gm.timestamp DESC")
    List<GroupMessage> findRecentByGroupIds(@Param("groupIds") List<Long> groupIds);

    @Modifying
    @Query("DELETE FROM GroupMessage gm WHERE gm.group.id = :groupId")
    void deleteByGroupId(@Param("groupId") Long groupId);

    @Query("SELECT gm.group.id, COUNT(gm) FROM GroupMessage gm " +
           "LEFT JOIN GroupMembership mem ON mem.group.id = gm.group.id AND mem.user.username = :username " +
           "WHERE gm.group.id IN :groupIds AND gm.sender.username <> :username " +
           "AND (mem.lastReadAt IS NULL OR gm.timestamp > mem.lastReadAt) " +
           "GROUP BY gm.group.id")
    List<Object[]> countUnreadByGroupsForUser(@Param("groupIds") List<Long> groupIds,
                                              @Param("username") String username);
}
