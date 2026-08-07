package com.example.testtitle.Services;

import com.example.testtitle.DTO.WatchRoomChatMessageDto;
import com.example.testtitle.DTO.WatchRoomControlRequest;
import com.example.testtitle.DTO.WatchRoomDto;
import com.example.testtitle.DTO.WatchRoomPlaylistDto;
import com.example.testtitle.DTO.WatchRoomPlaylistItemDto;
import com.example.testtitle.DTO.WatchRoomPreviewDto;
import com.example.testtitle.DTO.WatchRoomReactionDto;
import com.example.testtitle.DTO.WatchRoomSyncDto;
import com.example.testtitle.Models.User;
import com.example.testtitle.Models.WatchRoom;
import com.example.testtitle.Models.WatchRoomMessage;
import com.example.testtitle.Models.WatchRoomPlaylistItem;
import com.example.testtitle.Repositories.WatchRoomMessageRepository;
import com.example.testtitle.Repositories.WatchRoomPlaylistItemRepository;
import com.example.testtitle.Repositories.WatchRoomRepository;
import com.example.testtitle.enums.WatchRoomStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WatchRoomService {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final WatchRoomRepository roomRepository;
    private final WatchRoomMessageRepository messageRepository;
    private final WatchRoomPlaylistItemRepository playlistItemRepository;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Живое состояние комнат. Комната-источник правды для синхронизации:
     * status/positionMs/updatedAt по серверным часам.
     */
    private final Map<Long, RoomState> states = new ConcurrentHashMap<>();

    public WatchRoomService(WatchRoomRepository roomRepository,
                            WatchRoomMessageRepository messageRepository,
                            WatchRoomPlaylistItemRepository playlistItemRepository,
                            UserService userService,
                            SimpMessagingTemplate messagingTemplate) {
        this.roomRepository = roomRepository;
        this.messageRepository = messageRepository;
        this.playlistItemRepository = playlistItemRepository;
        this.userService = userService;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public WatchRoomDto createRoom(String username, String name) {
        User host = userService.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        String safeName = (name == null || name.isBlank()) ? "Комната " + username : name.trim();
        if (safeName.length() > 100) {
            safeName = safeName.substring(0, 100);
        }

        WatchRoom room = new WatchRoom(generateRoomCode(), safeName, host);
        final WatchRoom savedRoom = roomRepository.save(room);

        RoomState state = states.computeIfAbsent(savedRoom.getId(), id -> new RoomState(savedRoom));
        WatchRoomDto dto = toDto(savedRoom, state);

        sendAfterCommit(() -> {
            broadcastRoomsList();
            messagingTemplate.convertAndSendToUser(username, "/queue/room", dto);
        });
        return dto;
    }

    @Transactional
    public WatchRoomDto joinRoom(String username, String roomCode) {
        if (roomCode == null || roomCode.isBlank()) {
            throw new IllegalArgumentException("Код комнаты пустой");
        }
        WatchRoom room = roomRepository.lockRoomByCode(roomCode.trim().toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Комната не найдена"));
        room = roomRepository.findWithMembersById(room.getId())
                .orElseThrow(() -> new IllegalArgumentException("Комната не найдена"));

        if (!room.isMember(username)) {
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
            room.getMembers().add(user);
            room = roomRepository.save(room);
        }
        final WatchRoom joinedRoom = room;
        RoomState state = states.computeIfAbsent(joinedRoom.getId(), id -> new RoomState(joinedRoom));
        WatchRoomDto dto = toDto(joinedRoom, state);

        sendAfterCommit(() -> {
            messagingTemplate.convertAndSend("/topic/room." + joinedRoom.getId(), dto);
            messagingTemplate.convertAndSendToUser(username, "/queue/room", dto);
            broadcastRoomsList();
        });
        return dto;
    }

    @Transactional
    public WatchRoomDto leaveRoom(String username, Long roomId) {
        WatchRoom room = roomRepository.lockRoomById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Комната не найдена"));
        room = roomRepository.findWithMembersById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Комната не найдена"));
        if (!room.isMember(username)) {
            return null;
        }

        room.getMembers().removeIf(m -> m.getUsername().equals(username));

        if (room.getMembers().isEmpty()) {
            messageRepository.deleteByRoomId(roomId);
            playlistItemRepository.deleteByRoomId(roomId);
            roomRepository.delete(room);
            states.remove(roomId);
            sendAfterCommit(() -> {
                messagingTemplate.convertAndSend(
                        "/topic/room." + roomId,
                        new WatchRoomSyncDto(roomId, WatchRoomStatus.IDLE, 0, System.currentTimeMillis(), null, true)
                );
                broadcastRoomsList();
            });
            return null;
        }

        if (room.getHostUsername().equals(username)) {
            room.setHostUsername(room.getMembers().iterator().next().getUsername());
        }
        room = roomRepository.save(room);

        final WatchRoom updatedRoom = room;
        RoomState state = states.computeIfAbsent(roomId, id -> new RoomState(updatedRoom));
        WatchRoomDto dto = toDto(updatedRoom, state);
        sendAfterCommit(() -> {
            messagingTemplate.convertAndSend("/topic/room." + roomId, dto);
            broadcastRoomsList();
        });
        return dto;
    }

    /**
     * Только хост может менять состояние воспроизведения.
     */
    @Transactional
    public WatchRoomDto updateControl(String username, Long roomId, WatchRoomControlRequest request) {
        WatchRoom room = getRoom(roomId);
        if (!room.getHostUsername().equals(username)) {
            throw new IllegalArgumentException("Только хост может управлять воспроизведением");
        }

        RoomState state = states.computeIfAbsent(roomId, id -> new RoomState(room));

        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            state.status = parseStatus(request.getStatus());
        }
        if (request.getPositionMs() != null && request.getPositionMs() >= 0) {
            state.positionMs = request.getPositionMs();
        }
        if (request.getVideoUrl() != null && !request.getVideoUrl().isBlank()) {
            state.videoUrl = request.getVideoUrl().trim();
        }
        state.updatedAtMs = System.currentTimeMillis();

        room.setStatus(state.status);
        room.setPositionMs(state.positionMs);
        room.setVideoUrl(state.videoUrl);
        room.setUpdatedAt(Instant.ofEpochMilli(state.updatedAtMs));
        roomRepository.save(room);

        WatchRoomDto dto = toDto(room, state);
        dto.setRestart(Boolean.TRUE.equals(request.getRestart()));
        dto.setLastControlBy(username);
        System.out.println("[ctrl] user=" + username + " status=" + request.getStatus()
                + " posMs=" + request.getPositionMs() + " restart=" + dto.isRestart()
                + " -> room=" + roomId + " broadcast posMs=" + state.positionMs);
        sendAfterCommit(() -> messagingTemplate.convertAndSend("/topic/room." + roomId, dto));
        return dto;
    }

    @Transactional(readOnly = true)
    public WatchRoomDto requestState(String username, Long roomId) {
        WatchRoom room = getRoom(roomId);
        if (!room.isMember(username)) {
            throw new IllegalArgumentException("Вы не в этой комнате");
        }
        RoomState state = states.computeIfAbsent(roomId, id -> new RoomState(room));
        WatchRoomDto dto = toDto(room, state);
        sendAfterCommit(() -> messagingTemplate.convertAndSendToUser(username, "/queue/room-state", dto));
        return dto;
    }

    @Transactional
    public WatchRoomChatMessageDto sendChatMessage(String username, Long roomId, String content) {
        WatchRoom room = getRoom(roomId);
        if (!room.isMember(username)) {
            throw new IllegalArgumentException("Вы не в этой комнате");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Сообщение не может быть пустым");
        }

        String text = content.trim();
        if (text.length() > 2000) {
            text = text.substring(0, 2000);
        }

        User sender = userService.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        WatchRoomMessage message = new WatchRoomMessage();
        message.setContent(text);
        message.setSender(sender);
        message.setRoom(room);
        message.setTimestamp(LocalDateTime.now());

        WatchRoomChatMessageDto dto = toMessageDto(messageRepository.save(message));
        sendAfterCommit(() -> messagingTemplate.convertAndSend("/topic/room." + roomId + ".chat", dto));
        return dto;
    }

    @Transactional(readOnly = true)
    public WatchRoomReactionDto react(String username, Long roomId, String emoji) {
        WatchRoom room = getRoom(roomId);
        if (!room.isMember(username)) {
            throw new IllegalArgumentException("Вы не в этой комнате");
        }
        if (emoji == null || emoji.isBlank()) {
            throw new IllegalArgumentException("Реакция пустая");
        }
        String value = emoji.trim();
        if (value.length() > 8) {
            value = value.substring(0, 8);
        }
        WatchRoomReactionDto dto = new WatchRoomReactionDto(roomId, username, value, System.currentTimeMillis());
        sendAfterCommit(() -> messagingTemplate.convertAndSend("/topic/room." + roomId + ".reactions", dto));
        return dto;
    }

    @Transactional(readOnly = true)
    public List<WatchRoomChatMessageDto> getChatHistory(String username, Long roomId) {
        WatchRoom room = getRoom(roomId);
        if (!room.isMember(username)) {
            throw new IllegalArgumentException("Вы не в этой комнате");
        }
        List<WatchRoomChatMessageDto> result = new ArrayList<>();
        for (WatchRoomMessage message : messageRepository.findAllByRoomIdOrderByTimestampAsc(roomId)) {
            result.add(toMessageDto(message));
        }
        return result;
    }

    private WatchRoomChatMessageDto toMessageDto(WatchRoomMessage message) {
        String time = message.getTimestamp() != null
                ? message.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm"))
                : "";
        return new WatchRoomChatMessageDto(
                message.getId(),
                message.getRoom().getId(),
                message.getContent(),
                message.getSender().getUsername(),
                time
        );
    }

    @Transactional
    public WatchRoomPlaylistDto addPlaylistItem(String username, Long roomId, String url, String title) {
        WatchRoom room = getRoom(roomId);
        if (!room.isMember(username)) {
            throw new IllegalArgumentException("Вы не в этой комнате");
        }
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Ссылка на видео пустая");
        }
        String videoUrl = url.trim();
        if (videoUrl.length() > 2000) {
            videoUrl = videoUrl.substring(0, 2000);
        }

        List<WatchRoomPlaylistItem> existing = playlistItemRepository.findAllByRoomIdOrdered(roomId);
        int position = existing.isEmpty() ? 0 : existing.get(existing.size() - 1).getPosition() + 1;

        WatchRoomPlaylistItem item = new WatchRoomPlaylistItem();
        item.setVideoUrl(videoUrl);
        item.setTitle(title != null && !title.isBlank() ? title.trim().substring(0, Math.min(title.trim().length(), 300)) : null);
        item.setAddedBy(username);
        item.setPosition(position);
        item.setCreatedAt(LocalDateTime.now());
        item.setRoom(room);
        playlistItemRepository.save(item);

        WatchRoomPlaylistDto dto = toPlaylistDto(room, states.computeIfAbsent(roomId, id -> new RoomState(room)));
        sendAfterCommit(() -> messagingTemplate.convertAndSend("/topic/room." + roomId + ".playlist", dto));
        return dto;
    }

    @Transactional
    public WatchRoomPlaylistDto removePlaylistItem(String username, Long roomId, Long itemId) {
        WatchRoom room = getRoom(roomId);
        if (!room.getHostUsername().equals(username)) {
            throw new IllegalArgumentException("Только хост может удалять из очереди");
        }

        RoomState state = states.computeIfAbsent(roomId, id -> new RoomState(room));
        Optional<WatchRoomPlaylistItem> item = playlistItemRepository.findById(itemId);
        if (item.isEmpty() || !item.get().getRoom().getId().equals(roomId)) {
            throw new IllegalArgumentException("Элемент не найден");
        }
        playlistItemRepository.delete(item.get());

        if (state.currentItemId != null && state.currentItemId.equals(itemId)) {
            state.currentItemId = null;
            room.setCurrentItemId(null);
            roomRepository.save(room);
        }

        WatchRoomPlaylistDto dto = toPlaylistDto(room, state);
        sendAfterCommit(() -> messagingTemplate.convertAndSend("/topic/room." + roomId + ".playlist", dto));
        return dto;
    }

    @Transactional
    public WatchRoomDto playPlaylistItem(String username, Long roomId, Long itemId) {
        WatchRoom room = getRoom(roomId);
        if (!room.getHostUsername().equals(username)) {
            throw new IllegalArgumentException("Только хост может запускать видео из очереди");
        }
        WatchRoomPlaylistItem item = playlistItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Элемент не найден"));
        if (!item.getRoom().getId().equals(roomId)) {
            throw new IllegalArgumentException("Элемент не найден");
        }

        RoomState state = states.computeIfAbsent(roomId, id -> new RoomState(room));
        state.videoUrl = item.getVideoUrl();
        state.status = WatchRoomStatus.PLAYING;
        state.positionMs = 0;
        state.updatedAtMs = System.currentTimeMillis();
        state.currentItemId = item.getId();

        room.setVideoUrl(state.videoUrl);
        room.setStatus(state.status);
        room.setPositionMs(state.positionMs);
        room.setUpdatedAt(Instant.ofEpochMilli(state.updatedAtMs));
        room.setCurrentItemId(item.getId());
        roomRepository.save(room);

        WatchRoomDto dto = toDto(room, state);
        sendAfterCommit(() -> {
            messagingTemplate.convertAndSend("/topic/room." + roomId, dto);
            messagingTemplate.convertAndSend("/topic/room." + roomId + ".playlist", toPlaylistDto(room, state));
        });
        return dto;
    }

    @Transactional
    public WatchRoomDto nextPlaylistItem(String username, Long roomId) {
        WatchRoom room = getRoom(roomId);
        if (!room.getHostUsername().equals(username)) {
            throw new IllegalArgumentException("Только хост может переключать видео");
        }

        RoomState state = states.computeIfAbsent(roomId, id -> new RoomState(room));
        List<WatchRoomPlaylistItem> items = playlistItemRepository.findAllByRoomIdOrdered(roomId);
        if (items.isEmpty()) {
            return toDto(room, state);
        }

        WatchRoomPlaylistItem next = null;
        if (state.currentItemId == null) {
            next = items.get(0);
        } else {
            boolean foundCurrent = false;
            for (WatchRoomPlaylistItem item : items) {
                if (foundCurrent) {
                    next = item;
                    break;
                }
                if (item.getId().equals(state.currentItemId)) {
                    foundCurrent = true;
                }
            }
        }
        if (next == null) {
            state.status = WatchRoomStatus.PAUSED;
            state.updatedAtMs = System.currentTimeMillis();
            room.setStatus(state.status);
            room.setUpdatedAt(Instant.ofEpochMilli(state.updatedAtMs));
            roomRepository.save(room);
            WatchRoomDto dto = toDto(room, state);
            sendAfterCommit(() -> messagingTemplate.convertAndSend("/topic/room." + roomId, dto));
            return dto;
        }

        state.videoUrl = next.getVideoUrl();
        state.status = WatchRoomStatus.PLAYING;
        state.positionMs = 0;
        state.updatedAtMs = System.currentTimeMillis();
        state.currentItemId = next.getId();

        room.setVideoUrl(state.videoUrl);
        room.setStatus(state.status);
        room.setPositionMs(state.positionMs);
        room.setUpdatedAt(Instant.ofEpochMilli(state.updatedAtMs));
        room.setCurrentItemId(next.getId());
        roomRepository.save(room);

        WatchRoomDto dto = toDto(room, state);
        sendAfterCommit(() -> {
            messagingTemplate.convertAndSend("/topic/room." + roomId, dto);
            messagingTemplate.convertAndSend("/topic/room." + roomId + ".playlist", toPlaylistDto(room, state));
        });
        return dto;
    }

    @Transactional(readOnly = true)
    public WatchRoomPlaylistDto getPlaylist(String username, Long roomId) {
        WatchRoom room = getRoom(roomId);
        if (!room.isMember(username)) {
            throw new IllegalArgumentException("Вы не в этой комнате");
        }
        RoomState state = states.computeIfAbsent(roomId, id -> new RoomState(room));
        return toPlaylistDto(room, state);
    }

    @Transactional(readOnly = true)
    public List<WatchRoomPreviewDto> getRoomPreviews() {
        List<WatchRoom> rooms = roomRepository.findByOrderByCreatedAtDesc();
        List<WatchRoomPreviewDto> previews = new ArrayList<>();
        for (WatchRoom room : rooms) {
            previews.add(toPreview(room));
        }
        return previews;
    }

    @Transactional(readOnly = true)
    public List<WatchRoomPreviewDto> getRoomsForUser(String username) {
        List<WatchRoom> rooms = roomRepository.findAllByMemberUsername(username);
        List<WatchRoomPreviewDto> previews = new ArrayList<>();
        for (WatchRoom room : rooms) {
            previews.add(toPreview(room));
        }
        return previews;
    }

    /**
     * Периодическая рассылка состояния играющих комнат. Каждый пакет несёт
     * актуальную позицию (positionMs с поправкой на прошедшее время и
     * updatedAtMs=now), чтобы клиенты не перематывали на устаревшую позицию.
     */
    @Scheduled(fixedRate = 5000)
    public void syncBroadcast() {
        long now = System.currentTimeMillis();
        for (Map.Entry<Long, RoomState> entry : states.entrySet()) {
            RoomState state = entry.getValue();
            if (state.status == WatchRoomStatus.PLAYING) {
                long pos = state.positionMs;
                long delta = now - state.updatedAtMs;
                if (delta > 0 && delta < 60_000) {
                    pos += delta;
                }
                messagingTemplate.convertAndSend(
                        "/topic/room." + entry.getKey(),
                        new WatchRoomSyncDto(entry.getKey(), WatchRoomStatus.PLAYING, pos, now, state.videoUrl)
                );
            }
        }
    }

    private WatchRoom getRoom(Long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Комната не найдена"));
    }

    private String generateRoomCode() {
        Random random = new Random();
        for (int attempt = 0; attempt < 20; attempt++) {
            StringBuilder code = new StringBuilder(6);
            for (int i = 0; i < 6; i++) {
                code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
            }
            if (roomRepository.findByRoomCode(code.toString()).isEmpty()) {
                return code.toString();
            }
        }
        return String.valueOf(System.currentTimeMillis());
    }

    private WatchRoomStatus parseStatus(String status) {
        try {
            return WatchRoomStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Неизвестный статус: " + status);
        }
    }

    private WatchRoomDto toDto(WatchRoom room, RoomState state) {
        List<String> members = new ArrayList<>();
        for (User m : room.getMembers()) {
            members.add(m.getUsername());
        }
        return new WatchRoomDto(
                room.getId(),
                room.getRoomCode(),
                room.getName(),
                room.getHostUsername(),
                state.videoUrl,
                state.status,
                state.positionMs,
                state.updatedAtMs,
                members
        );
    }

    private WatchRoomPreviewDto toPreview(WatchRoom room) {
        RoomState state = states.computeIfAbsent(room.getId(), id -> new RoomState(room));
        return new WatchRoomPreviewDto(
                room.getId(),
                room.getRoomCode(),
                room.getName(),
                room.getHostUsername(),
                state.videoUrl,
                state.status,
                state.positionMs,
                room.getMembers().size()
        );
    }

    private WatchRoomPlaylistDto toPlaylistDto(WatchRoom room, RoomState state) {
        List<WatchRoomPlaylistItemDto> items = new ArrayList<>();
        for (WatchRoomPlaylistItem item : playlistItemRepository.findAllByRoomIdOrdered(room.getId())) {
            items.add(new WatchRoomPlaylistItemDto(
                    item.getId(),
                    item.getVideoUrl(),
                    item.getTitle(),
                    item.getAddedBy(),
                    item.getPosition()
            ));
        }
        return new WatchRoomPlaylistDto(room.getId(), state.currentItemId, items);
    }

    /**
     * Отправляет сообщения только после успешного commit транзакции, чтобы клиенты
     * не успевали отреагировать на ещё не закоммиченные данные (иначе повторная
     * команда от участника читает БД без его свежей записи о членстве).
     */
    private void sendAfterCommit(Runnable send) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send.run();
                }
            });
        } else {
            send.run();
        }
    }

    private void broadcastRoomsList() {
        messagingTemplate.convertAndSend("/topic/rooms", getRoomPreviews());
    }

    /**
     * Авторитетное живое состояние комнаты в памяти.
     */
    private static class RoomState {
        volatile WatchRoomStatus status;
        volatile long positionMs;
        volatile long updatedAtMs;
        volatile String videoUrl;
        volatile Long currentItemId;

        RoomState(WatchRoom room) {
            this.status = room.getStatus() != null ? room.getStatus() : WatchRoomStatus.IDLE;
            this.positionMs = room.getPositionMs();
            this.videoUrl = room.getVideoUrl();
            this.currentItemId = room.getCurrentItemId();
            this.updatedAtMs = room.getUpdatedAt() != null
                    ? room.getUpdatedAt().toEpochMilli()
                    : System.currentTimeMillis();
        }
    }
}
