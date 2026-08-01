package com.example.testtitle.Repositories;

import com.example.testtitle.Models.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("SELECT m FROM Message m WHERE "+
           "(m.sender.username = :u1 AND m.receiver.username = :u2) OR "+
            "(m.sender.username = :u2 AND m.receiver.username = :u1)"+
            "ORDER BY m.timestamp ASC")
    List<Message> findChatHistory(@Param("u1") String senderUsername, @Param("u2") String receiverUsername);

    @Query("SELECT m FROM Message m WHERE m.sender.username = :username OR m.receiver.username = :username ORDER BY m.timestamp DESC")
    List<Message> findAllByUserInvolvedOrderByTimestampDesc(@Param("username") String username);

    @Query("SELECT m.sender.username, COUNT(m) FROM Message m WHERE m.receiver.username = :username AND m.readAt IS NULL GROUP BY m.sender.username")
    List<Object[]> countUnreadByPartner(@Param("username") String username);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.receiver.username = :receiverUsername AND m.sender.username = :senderUsername AND m.readAt IS NULL")
    long countUnreadFromPartner(@Param("receiverUsername") String receiverUsername, @Param("senderUsername") String senderUsername);

    @Query("SELECT m FROM Message m WHERE m.receiver.username = :receiverUsername AND m.sender.username = :senderUsername AND m.readAt IS NULL")
    List<Message> findUnreadFromPartner(@Param("receiverUsername") String receiverUsername, @Param("senderUsername") String senderUsername);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Message m SET m.readAt = :readAt WHERE m.receiver.username = :receiverUsername AND m.sender.username = :senderUsername AND m.readAt IS NULL")
    int markConversationAsRead(@Param("receiverUsername") String receiverUsername,
                               @Param("senderUsername") String senderUsername,
                               @Param("readAt") LocalDateTime readAt);

    @Query("SELECT DISTINCT CASE WHEN m.sender.username = :username THEN m.receiver.username ELSE m.sender.username END " +
           "FROM Message m WHERE m.sender.username = :username OR m.receiver.username = :username")
    List<String> findConversationPartners(@Param("username") String username);

    @Modifying
    @Query("DELETE FROM Message m WHERE (m.sender.username = :u1 AND m.receiver.username = :u2) " +
           "OR (m.sender.username = :u2 AND m.receiver.username = :u1)")
    void deleteConversation(@Param("u1") String senderUsername, @Param("u2") String receiverUsername);
}
