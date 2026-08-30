package com.example.shadowvibe.Repositories;

import com.example.shadowvibe.Models.GroupInvite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupInviteRepository extends JpaRepository<GroupInvite, Long> {

    List<GroupInvite> findByInvitedUser_UsernameAndStatusOrderByCreatedAtDesc(
            String username, GroupInvite.Status status);

    Optional<GroupInvite> findByGroupIdAndInvitedUser_UsernameAndStatus(
            Long groupId, String username, GroupInvite.Status status);

    long countByInvitedUser_UsernameAndStatus(String username, GroupInvite.Status status);
}
