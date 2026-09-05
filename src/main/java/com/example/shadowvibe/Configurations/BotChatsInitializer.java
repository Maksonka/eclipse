package com.example.shadowvibe.Configurations;

import com.example.shadowvibe.Models.Message;
import com.example.shadowvibe.Models.User;
import com.example.shadowvibe.Repositories.MessageRepository;
import com.example.shadowvibe.Repositories.UserRepository;
import com.example.shadowvibe.enums.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@Order(2)
public class BotChatsInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BotChatsInitializer.class);

    private static final String[] BOT_USERNAMES = {
            "robo_alisa", "robo_artem", "robo_kira", "robo_max", "robo_sasha",
            "robo_dima", "robo_nika", "robo_timur", "robo_olya", "robo_igor",
            "robo_marina", "robo_dan", "robo_yana", "robo_evgen", "robo_valera",
            "robo_anya", "robo_fedor", "robo_lev", "robo_zina", "robo_misha"
    };

    private static final String[] OUTGOING_PHRASES = {
            "Привет! Всегда рад тебя видеть",
            "Ок, скину позже",
            "Давай, только коротко",
            "Договорились, до встречи",
            "Лови, что просил",
            "Видел, это круто!",
            "Я сегодня буду поздно",
            "Принято, держи в курсе",
            "На связи, пиши если что",
            "Отлично, так и сделаем",
            "Посмотрю и отвечу",
            "Спасибо, ты лучший!"
    };

    private static final String[] INCOMING_PHRASES = {
            "Привет! Как дела?",
            "Смотри, что я нашёл",
            "Го в кино на выходных?",
            "Кинь потом файл, ок?",
            "Спасибо, выручил!",
            "Видел наше новое видео?",
            "Ты дома сегодня?",
            "Когда созвон?",
            "Я всё отправил, проверь",
            "Клёво получилось!",
            "Как тебе новость?",
            "Позвони, когда освободишься"
    };

    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.bots.enabled:true}")
    private boolean botsEnabled;

    public BotChatsInitializer(UserRepository userRepository,
                               MessageRepository messageRepository,
                               PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!botsEnabled) {
            return;
        }
        Optional<User> optAdmin = userRepository.findByUsername(adminUsername);
        if (optAdmin.isEmpty()) {
            optAdmin = userRepository.findAllByOrderByUsernameAsc().stream()
                    .filter(u -> u.getRole() == UserRole.ADMIN)
                    .findFirst();
        }
        if (optAdmin.isEmpty()) {
            log.warn("BotChatsInitializer: администратор не найден, сид ботов пропущен");
            return;
        }
        User admin = optAdmin.get();
        List<User> bots = ensureBots();
        int created = 0;
        for (int i = 0; i < bots.size(); i++) {
            User bot = bots.get(i);
            if (messageRepository.existsConversationBetween(admin.getUsername(), bot.getUsername())) {
                continue;
            }
            List<Message> conversation = buildConversation(admin, bot, i);
            messageRepository.saveAll(conversation);
            created += conversation.size();
        }
        if (created > 0) {
            log.info("BotChatsInitializer: создано чатов с ботами - {}, сообщений - {}", bots.size(), created);
        }
    }

    private List<User> ensureBots() {
        List<User> bots = new ArrayList<>();
        for (String name : BOT_USERNAMES) {
            User bot = userRepository.findByUsername(name).orElseGet(() -> {
                User created = new User(name, name + "@shadowvibe.bot",
                        passwordEncoder.encode("botpassword"), UserRole.USER);
                created.setAbout("Я бот ShadowVibe, создан для тестов.");
                return userRepository.save(created);
            });
            bots.add(bot);
        }
        return bots;
    }

    private List<Message> buildConversation(User admin, User bot, int index) {
        LocalDateTime now = LocalDateTime.now();
        boolean unreadLast = index < 4;
        List<Message> msgs = new ArrayList<>();
        msgs.add(message(admin, bot, OUTGOING_PHRASES[index % OUTGOING_PHRASES.length],
                now.minusDays(7 + (index % 3)), true));
        msgs.add(message(bot, admin, INCOMING_PHRASES[index % INCOMING_PHRASES.length],
                now.minusDays(3 + (index % 4)).minusHours(index), true));
        if (unreadLast) {
            msgs.add(message(bot, admin, INCOMING_PHRASES[(index + 5) % INCOMING_PHRASES.length],
                    now.minusHours(index + 2), false));
        } else {
            msgs.add(message(admin, bot, OUTGOING_PHRASES[(index + 5) % OUTGOING_PHRASES.length],
                    now.minusHours(index + 2), true));
        }
        return msgs;
    }

    private Message message(User sender, User receiver, String content, LocalDateTime time, boolean read) {
        Message message = new Message(null, content, time, sender, receiver);
        if (read) {
            message.setReadAt(time.plusMinutes(3));
        }
        return message;
    }
}