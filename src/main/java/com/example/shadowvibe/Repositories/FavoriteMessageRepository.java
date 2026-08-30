package com.example.shadowvibe.Repositories;

import com.example.shadowvibe.Models.FavoriteMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteMessageRepository extends JpaRepository<FavoriteMessage, Long> {

    Optional<FavoriteMessage> findByUser_UsernameAndTargetTypeAndMessageId(
            @Param("username") String username,
            @Param("targetType") String targetType,
            @Param("messageId") Long messageId);

    List<FavoriteMessage> findAllByUser_UsernameOrderByFavoritedAtDesc(@Param("username") String username);

    @Query("SELECT f.messageId FROM FavoriteMessage f " +
           "JOIN Message m ON m.id = f.messageId " +
           "WHERE f.user.username = :username AND f.targetType = 'DIRECT' " +
           "AND ((m.sender.username = :username AND m.receiver.username = :partner) " +
           "OR (m.sender.username = :partner AND m.receiver.username = :username))")
    List<Long> findFavoritedIdsInDirectConversation(@Param("username") String username,
                                                    @Param("partner") String partner);

    @Query("SELECT f.messageId FROM FavoriteMessage f " +
           "JOIN GroupMessage gm ON gm.id = f.messageId " +
           "WHERE f.user.username = :username AND f.targetType = 'GROUP' AND gm.group.id = :groupId")
    List<Long> findFavoritedIdsInGroup(@Param("username") String username,
                                       @Param("groupId") Long groupId);

    long countByUser_Username(@Param("username") String username);

    @Modifying
    @Query("DELETE FROM FavoriteMessage f WHERE f.targetType = :targetType AND f.messageId = :messageId")
    void deleteAllByTargetTypeAndMessageId(@Param("targetType") String targetType,
                                           @Param("messageId") Long messageId);

    @Modifying
    @Query("DELETE FROM FavoriteMessage f WHERE f.targetType = 'DIRECT' AND f.messageId IN (" +
           "SELECT m.id FROM Message m WHERE " +
           "(m.sender.username = :u1 AND m.receiver.username = :u2) " +
           "OR (m.sender.username = :u2 AND m.receiver.username = :u1))")
    void deleteFavoritesOfConversation(@Param("u1") String username,
                                       @Param("u2") String partnerUsername);

    @Modifying
    @Query("DELETE FROM FavoriteMessage f WHERE f.targetType = 'GROUP' AND f.messageId IN (" +
           "SELECT gm.id FROM GroupMessage gm WHERE gm.group.id = :groupId)")
    void deleteFavoritesOfGroup(@Param("groupId") Long groupId);
}
