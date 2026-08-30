package com.example.shadowvibe.Repositories;

import com.example.shadowvibe.Models.GroupMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupMembershipRepository extends JpaRepository<GroupMembership, Long> {

    Optional<GroupMembership> findByGroupIdAndUserUsername(Long groupId, String username);

    List<GroupMembership> findByUserUsername(String username);

    void deleteByGroupId(Long groupId);

    void deleteByGroupIdAndUserUsername(Long groupId, String username);

    boolean existsByGroupIdAndUserUsernameAndMutedTrue(Long groupId, String username);

    @Query("select m.group.id from GroupMembership m where m.user.username = :username and m.muted = true")
    List<Long> findMutedGroupIdsByUserUsername(@Param("username") String username);

    @Query("select count(m) from GroupMembership m where m.user.username = :usernameA " +
           "and m.group.id in (select m2.group.id from GroupMembership m2 where m2.user.username = :usernameB)")
    long countSharedGroups(@Param("usernameA") String usernameA, @Param("usernameB") String usernameB);
}
