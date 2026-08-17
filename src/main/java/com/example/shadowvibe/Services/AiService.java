package com.example.shadowvibe.Services;

import com.example.shadowvibe.DTO.MessageSearchResultDto;
import com.example.shadowvibe.Models.GroupMessage;
import com.example.shadowvibe.Models.GroupMembership;
import com.example.shadowvibe.Models.Message;
import com.example.shadowvibe.Models.User;
import com.example.shadowvibe.Repositories.GroupMembershipRepository;
import com.example.shadowvibe.Repositories.GroupMessageRepository;
import com.example.shadowvibe.Repositories.MessageRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AiService {

    private static final Set<String> STOPWORDS = Set.of(
            "и", "в", "во", "не", "что", "он", "на", "я", "с", "со", "как", "а", "то", "все", "она",
            "так", "его", "но", "да", "ты", "к", "у", "же", "вы", "за", "бы", "по", "ее", "при", "или",
            "это", "еще", "меня", "мне", "этот", "был", "была", "были", "быть", "о", "от", "из", "об",
            "для", "их", "мы", "них", "там", "тут", "где", "когда", "потому", "тот", "какой", "будет",
            "можно", "нужно", "надо", "очень", "уже", "даже", "только", "чтобы", "также", "почему",
            "зачем", "ли", "сейчас", "теперь", "тоже", "потом", "снова", "опять", "сегодня", "завтра",
            "который", "которая", "которые", "сам", "сама", "самое", "один", "одна", "если", "нибудь",
            "привет", "здравствуй", "спасибо", "пожалуйста", "ок", "окей", "ага", "угу", "ну", "эх", "вот"
    );

    private static final List<String> STYLE_REPLIES = List.of(
            "Да, согласен 👍",
            "Кстати, а что думаешь?",
            "Давай обсудим вечером)",
            "Отлично, договорились!",
            "Хм, надо подумать. Напишу позже.",
            "Точно! Я как раз об этом думал."
    );

    private final MessageRepository messageRepository;
    private final GroupMessageRepository groupMessageRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final UserService userService;
    private final MessageService messageService;
    private final GroupService groupService;
    private final TranslationService translationService;

    public AiService(MessageRepository messageRepository,
                     GroupMessageRepository groupMessageRepository,
                     GroupMembershipRepository groupMembershipRepository,
                     UserService userService,
                     MessageService messageService,
                     GroupService groupService,
                     TranslationService translationService) {
        this.messageRepository = messageRepository;
        this.groupMessageRepository = groupMessageRepository;
        this.groupMembershipRepository = groupMembershipRepository;
        this.userService = userService;
        this.messageService = messageService;
        this.groupService = groupService;
        this.translationService = translationService;
    }

    public Map<String, Object> digest(String username, long sinceEpochMillis) {
        LocalDateTime since = sinceEpochMillis > 0
                ? LocalDateTime.ofInstant(Instant.ofEpochMilli(sinceEpochMillis), ZoneId.systemDefault())
                : LocalDateTime.now().minusHours(24);
        LocalDateTime now = LocalDateTime.now();

        List<Message> direct = messageRepository.findSinceInvolving(username, since).stream()
                .filter(m -> visibleDirect(m, username))
                .collect(Collectors.toList());

        List<GroupMessage> group = new ArrayList<>();
        User user = userService.findByUsername(username).orElse(null);
        if (user != null) {
            List<Long> groupIds = groupMembershipRepository.findByUserUsername(username).stream()
                    .map(GroupMembership::getGroup)
                    .filter(g -> g != null)
                    .map(g -> g.getId())
                    .collect(Collectors.toList());
            if (!groupIds.isEmpty()) {
                group = groupMessageRepository.findByGroupIdsAndTimestampAfter(groupIds, since).stream()
                        .filter(gm -> !gm.isDeletedByUser(user.getId()))
                        .collect(Collectors.toList());
            }
        }

        int directCount = direct.size();
        int groupCount = group.size();
        int total = directCount + groupCount;

        Map<String, Integer> senders = new LinkedHashMap<>();
        List<String> allTexts = new ArrayList<>();
        for (Message m : direct) {
            countSender(senders, m.getSender());
            if (hasText(m) && !isEncrypted(m.getContent())) {
                allTexts.add(m.getContent());
            }
        }
        for (GroupMessage gm : group) {
            countSender(senders, gm.getSender());
            if (hasText(gm.getContent()) && !isEncrypted(gm.getContent())) {
                allTexts.add(gm.getContent());
            }
        }
        senders.remove(username);

        List<String> topics = extractTopics(allTexts, username, 3);
        String topSender = senders.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        long mentionCount = 0;
        List<String> mentionQuotes = new ArrayList<>();
        String mentionPattern = "@" + username;
        for (Message m : direct) {
            if (m.getContent() != null && !isEncrypted(m.getContent())
                    && m.getContent().toLowerCase(Locale.ROOT).contains(mentionPattern.toLowerCase(Locale.ROOT))) {
                mentionCount++;
                if (mentionQuotes.size() < 2 && hasText(m)) {
                    mentionQuotes.add(m.getSender() == null ? "?" : m.getSender().getUsername() + ": «" + trimQuote(m.getContent()) + "»");
                }
            }
        }
        for (GroupMessage gm : group) {
            if (gm.getContent() != null && !isEncrypted(gm.getContent())
                    && gm.getContent().toLowerCase(Locale.ROOT).contains(mentionPattern.toLowerCase(Locale.ROOT))) {
                mentionCount++;
                if (mentionQuotes.size() < 2 && hasText(gm.getContent())) {
                    mentionQuotes.add(gm.getSender() == null ? "?" : gm.getSender().getUsername() + ": «" + trimQuote(gm.getContent()) + "»");
                }
            }
        }

        String text = composeDigest(since, now, total, directCount, groupCount, topics, topSender, mentionCount, mentionQuotes);

        Map<String, Object> result = new HashMap<>();
        result.put("text", text);
        result.put("total", total);
        result.put("directCount", directCount);
        result.put("groupCount", groupCount);
        result.put("topics", topics);
        result.put("topSender", topSender);
        result.put("mentionCount", mentionCount);
        result.put("mock", false);
        return result;
    }

    public String assistantReply(String username, String chatType, String target, String rawQuery) {
        String q = normalizeQuery(rawQuery);
        String lower = q.toLowerCase(Locale.ROOT);

        if (lower.contains("поиск") || lower.contains("найди") || lower.contains("ищи") || lower.contains("search")) {
            String searchText = extractAfter(q, new String[]{"поиск", "найди", "ищи", "search"});
            if (searchText.isBlank()) {
                return "Напиши, что ищем: например, «найди что-нибудь про файлы».";
            }
            return searchInChat(username, chatType, target, searchText);
        }

        if (lower.contains("переведи") || lower.contains("перевод")) {
            String text = extractAfter(q, new String[]{"переведи", "перевод"});
            if (text.isBlank()) {
                return "Напиши текст после «переведи», например: «переведи hi bro».";
            }
            String translated = translationService.translateAuto(text);
            if (translated.equalsIgnoreCase(text)) {
                return "Не нашёл перевод слов в сообщении. Словарь пополняется — попробуй более простую фразу.";
            }
            return "Перевод: «" + translated + "»";
        }

        if (lower.contains("транскрипци") || lower.contains("расшифруй") || lower.contains("голосов")) {
            return transcribeVoices(username, chatType, target);
        }

        if (lower.contains("ответ") || lower.contains("напиши за меня") || lower.contains("сгенерируй")) {
            return styleReply(username, chatType, target);
        }

        return "Я — AI-ассистент. Команды:\n" +
                "• «найди …» — поиск по истории чата\n" +
                "• «переведи …» — перевод текста\n" +
                "• «напиши ответ» — ответ в твоём стиле\n" +
                "• «расшифруй голосовые» — содержание чата";
    }

    public String translateText(String text, String target) {
        String translated = translationService.translateAuto(text);
        return translated == null ? text : translated;
    }

    private String transcribeVoices(String username, String chatType, String target) {
        List<String> found = new ArrayList<>();
        if ("group".equals(chatType)) {
            Long groupId;
            try {
                groupId = Long.parseLong(target);
            } catch (NumberFormatException e) {
                return "Не удалось определить группу.";
            }
            for (GroupMessage gm : groupMessageRepository.findByGroupIdOrderByTimestampAsc(groupId)) {
                if (gm.getContent() != null && !gm.getContent().isBlank()) {
                    found.add(trimQuote(gm.getContent()));
                }
            }
        } else {
            for (Message m : messageRepository.findChatHistory(username, target)) {
                if (m.getContent() != null && !m.getContent().isBlank() && !isEncrypted(m.getContent())) {
                    found.add(trimQuote(m.getContent()));
                }
            }
        }
        if (found.isEmpty()) {
            return "Голосовых сообщений в этом чате не нашлось — все текстовые, либо сообщения зашифрованы (E2E расшифровку делаю только на клиенте).";
        }
        StringBuilder sb = new StringBuilder("Содержимое чата (по последним сообщениям):");
        for (String s : found.subList(Math.max(0, found.size() - 5), found.size())) {
            sb.append("\n• «").append(s).append("»");
        }
        sb.append("\n(распознавание речи подключу при наличии STT-движка — пока показываю текст сообщений)");
        return sb.toString();
    }

    private String searchInChat(String username, String chatType, String target, String searchText) {
        List<MessageSearchResultDto> results;
        if ("group".equals(chatType)) {
            Long groupId;
            try {
                groupId = Long.parseLong(target);
            } catch (NumberFormatException e) {
                return "Не удалось определить группу для поиска.";
            }
            results = groupService.searchGroupHistory(groupId, username, searchText, 3);
        } else {
            results = messageService.searchDirectHistory(username, target, searchText, 3);
        }
        if (results == null || results.isEmpty()) {
            return "Ничего не нашёл по запросу «" + searchText + "» в этом чате.";
        }
        StringBuilder sb = new StringBuilder("Нашёл в чате:");
        for (MessageSearchResultDto r : results) {
            sb.append("\n• «").append(trimQuote(r.getContent())).append("» — ")
                    .append(r.getSenderUsername()).append(", ").append(r.getDate() == null ? "" : r.getDate());
        }
        return sb.toString();
    }

    private String styleReply(String username, String chatType, String target) {
        List<String> own = recentOwnTexts(username, chatType, target);
        String template = STYLE_REPLIES.get(Math.abs((target + username).hashCode()) % STYLE_REPLIES.size());
        if (own.isEmpty()) {
            return "Ты пока не писал в этом чате — не из чего уловить стиль. Напиши пару сообщений, и я предложу ответ в твоём стиле.";
        }
        return "Твой стиль — короткие сообщения, последнее: «" + trimQuote(own.get(own.size() - 1)) + "». Предлагаю ответ: «" + template + "»";
    }

    private List<String> recentOwnTexts(String username, String chatType, String target) {
        List<String> texts = new ArrayList<>();
        if ("group".equals(chatType)) {
            Long groupId;
            try {
                groupId = Long.parseLong(target);
            } catch (NumberFormatException e) {
                return texts;
            }
            for (GroupMessage gm : groupMessageRepository.findByGroupIdOrderByTimestampAsc(groupId)) {
                if (gm.getSender() != null && username.equals(gm.getSender().getUsername()) && hasText(gm.getContent())) {
                    texts.add(gm.getContent());
                }
            }
        } else {
            for (Message m : messageRepository.findChatHistory(username, target)) {
                if (m.getSender() != null && username.equals(m.getSender().getUsername()) && hasText(m)) {
                    texts.add(m.getContent());
                }
            }
        }
        return texts;
    }

    private String composeDigest(LocalDateTime since, LocalDateTime now, int total,
                                 int directCount, int groupCount, List<String> topics,
                                 String topSender, long mentionCount, List<String> mentionQuotes) {
        long minutes = Math.max(0, Duration.between(since, now).toMinutes());
        String period;
        if (minutes < 90) {
            period = "за последний час";
        } else if (minutes < 24 * 60) {
            period = "за " + Math.round(minutes / 60.0) + " ч";
        } else {
            period = "за " + Math.max(1, minutes / (24 * 60)) + " дн";
        }

        StringBuilder sb = new StringBuilder();
        if (total == 0) {
            sb.append("За этот период новых сообщений не было. Все спокойно!");
        } else {
            String word = plural(total, "сообщение", "сообщения", "сообщений");
            sb.append(period).append(" — ").append(total).append(" ").append(word).append(". ");
            if (!topics.isEmpty()) {
                sb.append("Обсуждали: ").append(String.join(", ", topics)).append(". ");
            }
            if (directCount > 0 && groupCount > 0) {
                sb.append("Личные чаты: ").append(directCount).append(", группы: ").append(groupCount).append(". ");
            }
            if (mentionQuotes.isEmpty() && mentionCount > 0) {
                sb.append("Тебя упоминали ").append(mentionCount).append(" ").append(plural(mentionCount, "раз", "раза", "раз")).append(". ");
            }
            for (String quote : mentionQuotes) {
                sb.append(quote).append(" ");
            }
            if (topSender != null) {
                sb.append("Больше всех писал: ").append(topSender).append(". ");
            }
        }
        return sb.toString();
    }

    private boolean visibleDirect(Message m, String username) {
        boolean own = m.getSender() != null && username.equals(m.getSender().getUsername());
        return own ? !m.isDeletedBySender() : !m.isDeletedByReceiver();
    }

    private boolean isEncrypted(String content) {
        return content != null && content.startsWith("e2e1:");
    }

    private boolean hasText(Message m) {
        return hasText(m.getContent());
    }

    private boolean hasText(String s) {
        return s != null && !s.isBlank() && !"Сообщение удалено".equals(s.trim());
    }

    private void countSender(Map<String, Integer> senders, User sender) {
        if (sender != null && sender.getUsername() != null) {
            senders.merge(sender.getUsername(), 1, Integer::sum);
        }
    }

    private List<String> extractTopics(List<String> texts, String username, int limit) {
        Map<String, Integer> freq = new HashMap<>();
        for (String text : texts) {
            String normalized = text.toLowerCase(Locale.ROOT)
                    .replaceAll("[^а-яёa-z0-9 ]", " ")
                    .trim();
            for (String token : normalized.split("\\s+")) {
                if (token.length() < 4) {
                    continue;
                }
                if (STOPWORDS.contains(token) || token.equals(username.toLowerCase(Locale.ROOT))) {
                    continue;
                }
                freq.merge(token, 1, Integer::sum);
            }
        }
        return freq.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private String normalizeQuery(String q) {
        if (q == null) {
            return "";
        }
        String s = q.trim().replaceAll("(?i)^(ai|@ai|/ai|ассистент)[,:\\s]+", "");
        return s.trim();
    }

    private String extractAfter(String q, String[] keywords) {
        String lower = q.toLowerCase(Locale.ROOT);
        int best = -1;
        int bestLen = -1;
        for (String kw : keywords) {
            int idx = lower.indexOf(kw);
            if (idx >= 0 && kw.length() > bestLen) {
                best = idx;
                bestLen = kw.length();
            }
        }
        if (best < 0) {
            return q;
        }
        String rest = q.substring(best + bestLen).trim();
        return rest.replaceFirst("^[,:\\-—\\s]+", "").trim();
    }

    private String trimQuote(String s) {
        if (s == null) {
            return "";
        }
        String t = s.trim();
        return t.length() > 90 ? t.substring(0, 90) + "…" : t;
    }

    private String plural(long n, String one, String few, String many) {
        long mod10 = n % 10;
        long mod100 = n % 100;
        if (mod10 == 1 && mod100 != 11) {
            return one;
        }
        if (mod10 >= 2 && mod10 <= 4 && (mod100 < 10 || mod100 >= 20)) {
            return few;
        }
        return many;
    }
}
