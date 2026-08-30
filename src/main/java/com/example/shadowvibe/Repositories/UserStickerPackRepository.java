package com.example.shadowvibe.Repositories;

import com.example.shadowvibe.Models.UserStickerPack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface UserStickerPackRepository extends JpaRepository<UserStickerPack, Long> {

    boolean existsByUserIdAndPackId(Long userId, Long packId);

    @Query("SELECT u.pack.id FROM UserStickerPack u WHERE u.user.id = :userId")
    Set<Long> findPackIdsByUserId(@Param("userId") Long userId);
}
