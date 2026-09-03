package com.example.shadowvibe.Repositories;

import com.example.shadowvibe.Models.GhostException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GhostExceptionRepository extends JpaRepository<GhostException, Long> {

    @Query("SELECT ge FROM GhostException ge WHERE ge.user.username = :username")
    List<GhostException> findByUsername(@Param("username") String username);

    @Query("SELECT ge FROM GhostException ge WHERE ge.user.username = :username AND ge.exceptionUser.username = :exceptionUsername")
    Optional<GhostException> findByUsernameAndExceptionUsername(
            @Param("username") String username,
            @Param("exceptionUsername") String exceptionUsername);

    @Modifying
    @Query("DELETE FROM GhostException ge WHERE ge.user.username = :username AND ge.exceptionUser.username = :exceptionUsername")
    int deleteByUsernameAndExceptionUsername(
            @Param("username") String username,
            @Param("exceptionUsername") String exceptionUsername);
}
