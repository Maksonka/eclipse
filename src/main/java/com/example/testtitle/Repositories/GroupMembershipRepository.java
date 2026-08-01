package com.example.testtitle.Repositories;

import com.example.testtitle.Models.GroupMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupMembershipRepository extends JpaRepository<GroupMembership, Long> {

    Optional<GroupMembership> findByGroupIdAndUserUsername(Long groupId, String username);

    List<GroupMembership> findByUserUsername(String username);

    void deleteByGroupId(Long groupId);

    void deleteByGroupIdAndUserUsername(Long groupId, String username);
}
