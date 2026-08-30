package com.example.shadowvibe.Repositories;

import com.example.shadowvibe.Models.ChatGroup;
import com.example.shadowvibe.Models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatGroupRepository extends JpaRepository<ChatGroup, Long> {

    @Query("SELECT DISTINCT g FROM ChatGroup g JOIN g.members m WHERE m.username = :username ORDER BY g.name ASC")
    List<ChatGroup> findAllByMemberUsername(@Param("username") String username);

    @Query("SELECT DISTINCT g FROM ChatGroup g JOIN g.members m1 JOIN g.members m2 WHERE m1.username = :u1 AND m2.username = :u2 ORDER BY g.name ASC")
    List<ChatGroup> findCommonGroups(@Param("u1") String u1, @Param("u2") String u2);
}
