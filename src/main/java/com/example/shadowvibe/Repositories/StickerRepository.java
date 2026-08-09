package com.example.shadowvibe.Repositories;

import com.example.shadowvibe.Models.Sticker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StickerRepository extends JpaRepository<Sticker, Long> {

    Optional<Sticker> findByCode(String code);

    long countByPackId(Long packId);
}
