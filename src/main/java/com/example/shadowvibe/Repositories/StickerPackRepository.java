package com.example.shadowvibe.Repositories;

import com.example.shadowvibe.Models.StickerPack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StickerPackRepository extends JpaRepository<StickerPack, Long> {

    Optional<StickerPack> findByNameIgnoreCase(String name);

    List<StickerPack> findAllByOrderByCreatedAtAsc();
}
