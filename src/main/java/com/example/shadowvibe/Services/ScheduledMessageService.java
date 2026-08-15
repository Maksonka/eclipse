package com.example.shadowvibe.Services;

import com.example.shadowvibe.DTO.ScheduleMessageRequest;
import com.example.shadowvibe.DTO.ScheduledMessageDto;
import com.example.shadowvibe.Models.ChatGroup;
import com.example.shadowvibe.Models.Message;
import com.example.shadowvibe.Models.ScheduledMessage;
import com.example.shadowvibe.Models.User;
import com.example.shadowvibe.Repositories.ChatGroupRepository;
import com.example.shadowvibe.Repositories.ScheduledMessageRepository;
import com.example.shadowvibe.Repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class ScheduledMessageService {

    private static final Logger log = LoggerFactory.getLogger(ScheduledMessageService.class);
    private static final long MIN_DELAY_MS = 10_000L;
    private static final long MAX_DELAY_MS = 365L * 24 * 60 * 60 * 1000;

    private final ScheduledMessageRepository scheduledMessageRepository;
    private final UserRepository userRepository;
    private final ChatGroupRepository chatGroupRepository;
    private final MessageService messageService;
    private final GroupService groupService;

    public ScheduledMessageService(ScheduledMessageRepository scheduledMessageRepository,
                                   UserRepository userRepository,
                                   ChatGroupRepository chatGroupRepository,
                                   MessageService messageService,
                                   GroupService groupService) {
        this.scheduledMessageRepository = scheduledMessageRepository;
        this.userRepository = userRepository;
        this.chatGroupRepository = chatGroupRepository;
        this.messageService = messageService;
        this.groupService = groupService;
    }

    public ScheduledMessageDto schedule(String username, ScheduleMessageRequest request) {
        if (request.getScheduleAt() == null) {
            throw new IllegalArgumentException("Не указано время отправки");
        }
        long now = System.currentTimeMillis();
        if (request.getScheduleAt() < now + MIN_DELAY_MS) {
            throw new IllegalArgumentException("Время отправки должно быть в будущем (минимум через 10 секунд)");
        }
        if (request.getScheduleAt() > now + MAX_DELAY_MS) {
            throw new IllegalArgumentException("Можно планировать не дальше чем на год вперед");
        }

        String content = request.getContent() == null ? "" : request.getContent().trim();
        if (content.isEmpty()) {
            throw new IllegalArgumentException("Сообщение не может быть пустым");
        }

        String targetType = request.getTargetType() == null ? "" : request.getTargetType().toUpperCase();
        if (!"DIRECT".equals(targetType) && !"GROUP".equals(targetType)) {
            throw new IllegalArgumentException("Неверный тип получателя");
        }

        User sender = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        if ("DIRECT".equals(targetType)) {
            if (request.getReceiverUsername() == null || request.getReceiverUsername().isBlank()) {
                throw new IllegalArgumentException("Не указан получатель");
            }
            if (userRepository.findByUsername(request.getReceiverUsername()).isEmpty()) {
                throw new IllegalArgumentException("Получатель не найден: " + request.getReceiverUsername());
            }
        } else {
            if (request.getGroupId() == null) {
                throw new IllegalArgumentException("Не указана группа");
            }
            groupService.getGroupForMember(request.getGroupId(), username);
        }

        ScheduledMessage entity = new ScheduledMessage();
        entity.setSender(sender);
        entity.setTargetType(targetType);
        entity.setReceiverUsername(request.getReceiverUsername());
        entity.setGroupId(request.getGroupId());
        entity.setContent(content);
        entity.setReplyToMessageId(request.getReplyToMessageId());
        entity.setScheduleAt(toLocal(request.getScheduleAt()));
        entity.setStatus(ScheduledMessage.STATUS_PENDING);
        entity.setCreatedAt(LocalDateTime.now());
        ScheduledMessage saved = scheduledMessageRepository.save(entity);
        log.info("ScheduledMessage created id={} user={} target={} at={}", saved.getId(), username, targetType, saved.getScheduleAt());
        return toDto(saved);
    }

    public List<ScheduledMessageDto> list(String username) {
        return scheduledMessageRepository
                .findBySenderUsernameAndStatusOrderByScheduleAtAsc(username, ScheduledMessage.STATUS_PENDING)
                .stream().map(this::toDto).toList();
    }

    public boolean cancel(Long id, String username) {
        ScheduledMessage entity = scheduledMessageRepository.findById(id).orElse(null);
        if (entity == null || !entity.getSender().getUsername().equals(username)) {
            return false;
        }
        if (!ScheduledMessage.STATUS_PENDING.equals(entity.getStatus())) {
            return false;
        }
        entity.setStatus(ScheduledMessage.STATUS_CANCELLED);
        scheduledMessageRepository.save(entity);
        log.info("ScheduledMessage cancelled id={} user={}", id, username);
        return true;
    }

    @Scheduled(fixedDelay = 10_000)
    @Transactional
    public void processDue() {
        List<ScheduledMessage> due = scheduledMessageRepository
                .findByStatusAndScheduleAtLessThanEqual(ScheduledMessage.STATUS_PENDING, LocalDateTime.now());
        if (due.isEmpty()) {
            return;
        }
        for (ScheduledMessage message : due) {
            try {
                send(message);
                message.setStatus(ScheduledMessage.STATUS_SENT);
                message.setSentAt(LocalDateTime.now());
                message.setErrorMessage(null);
            } catch (Exception e) {
                message.setStatus(ScheduledMessage.STATUS_FAILED);
                message.setErrorMessage(truncate(e.getMessage(), 500));
                log.warn("ScheduledMessage {} failed: {}", message.getId(), e.toString());
            }
            scheduledMessageRepository.save(message);
        }
    }

    private void send(ScheduledMessage message) {
        String sender = message.getSender().getUsername();
        if ("DIRECT".equals(message.getTargetType())) {
            Message sent = messageService.saveMessage(
                    sender,
                    message.getReceiverUsername(),
                    message.getContent(),
                    message.getReplyToMessageId()
            );
            messageService.broadcastDirectMessage(sent);
        } else {
            groupService.broadcastGroupMessage(groupService.saveGroupMessage(
                    sender,
                    message.getGroupId(),
                    message.getContent(),
                    message.getReplyToMessageId()
            ));
        }
    }

    private ScheduledMessageDto toDto(ScheduledMessage entity) {
        ScheduledMessageDto dto = new ScheduledMessageDto();
        dto.setId(entity.getId());
        dto.setTargetType(entity.getTargetType());
        dto.setTargetName(resolveTargetName(entity));
        dto.setContent(entity.getContent());
        dto.setReplyToMessageId(entity.getReplyToMessageId());
        dto.setScheduleAt(toEpochMillis(entity.getScheduleAt()));
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(toEpochMillis(entity.getCreatedAt()));
        return dto;
    }

    private String resolveTargetName(ScheduledMessage entity) {
        if ("GROUP".equals(entity.getTargetType())) {
            return chatGroupRepository.findById(entity.getGroupId())
                    .map(ChatGroup::getName)
                    .orElse("Группа #" + entity.getGroupId());
        }
        return entity.getReceiverUsername();
    }

    private static LocalDateTime toLocal(long epochMillis) {
        return Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private static Long toEpochMillis(LocalDateTime dateTime) {
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
