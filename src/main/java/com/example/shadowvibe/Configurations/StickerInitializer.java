package com.example.shadowvibe.Configurations;

import com.example.shadowvibe.Models.Sticker;
import com.example.shadowvibe.Models.StickerPack;
import com.example.shadowvibe.Repositories.StickerPackRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class StickerInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StickerInitializer.class);

    private final StickerPackRepository stickerPackRepository;

    public StickerInitializer(StickerPackRepository stickerPackRepository) {
        this.stickerPackRepository = stickerPackRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedIfAbsent("Настроение",
                "smile", "laugh", "heart", "wink");
        seedIfAbsent("Реакции",
                "thumbsup", "party", "fire", "ok");
    }

    private void seedIfAbsent(String packName, String... stickerNames) {
        if (stickerPackRepository.findByNameIgnoreCase(packName).isPresent()) {
            return;
        }
        StickerPack pack = stickerPackRepository.save(new StickerPack(packName, null));
        int i = 0;
        for (String name : stickerNames) {
            i++;
            Sticker sticker = new Sticker("pack" + pack.getId() + "_" + i, "/img/stickers/" + name + ".svg");
            pack.addSticker(sticker);
        }
        stickerPackRepository.save(pack);
        log.info("Создан системный набор стикеров: {}", packName);
    }
}
