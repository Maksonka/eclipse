package com.example.shadowvibe.Services;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class TranscriptionService {

    private static final int MAX_LENGTH = 3500;

    private static final List<String> PHRASES = List.of(
            "Привет, как у тебя дела?",
            "Слушай, я сегодня буду чуть позже.",
            "Давай созвонимся вечером после восьми.",
            "Я уже видел то самое видео, ты прав.",
            "Не переживай, всё будет хорошо.",
            "Можешь скинуть адрес ещё раз?",
            "Я наконец-то дома, гуляли долго.",
            "Завтра точно получится, обещаю.",
            "Спасибо большое за помощь, выручил.",
            "Погода сегодня просто шикарная, солнце весь день.",
            "А ты уже смотрел новый трейлер?",
            "Я заметил, что у тебя новое фото на аватаре.",
            "Говорил же, не стоит переживать заранее.",
            "Кстати, хотел спросить про твой отпуск.",
            "Вспомнил, мне нужно позвонить коллеге.",
            "Напиши мне, когда будешь свободен.",
            "Наверное, лучше перенести на неделю.",
            "Хорошо, договорились, до встречи!",
            "Мне нужно пару минут, я перезвоню.",
            "Точно помнишь, в котором часу начало?"
    );

    public String transcribe(Long durationMs) {
        int count = phrasesCount(durationMs);
        StringBuilder sb = new StringBuilder();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (int i = 0; i < count; i++) {
            String phrase = PHRASES.get(rnd.nextInt(PHRASES.size()));
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(phrase);
        }
        if (sb.length() > MAX_LENGTH) {
            return sb.substring(0, MAX_LENGTH).trim() + "…";
        }
        return sb.toString();
    }

    private int phrasesCount(Long durationMs) {
        if (durationMs == null || durationMs <= 0) {
            return ThreadLocalRandom.current().nextInt(1, 3);
        }
        int count = (int) Math.ceil(durationMs / 4000.0);
        return Math.max(1, Math.min(count, 12));
    }
}