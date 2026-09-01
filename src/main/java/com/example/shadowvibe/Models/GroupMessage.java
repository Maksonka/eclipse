package com.example.shadowvibe.Models;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "group_messages")
public class GroupMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "attachment_filename")
    private String attachmentFilename;

    @Column(name = "attachment_type")
    private String attachmentType;

    @Column(name = "attachment_original_name")
    private String attachmentOriginalName;

    @Column(name = "attachment_size")
    private Long attachmentSize;

    @Column(name = "audio_url", length = 500)
    private String audioUrl;

    @Column(name = "audio_duration_ms")
    private Long audioDurationMs;

    @Column(name = "reply_to_id")
    private Long replyToMessageId;

    @Column(name = "reply_to_content", length = 200)
    private String replyToContent;

    @Column(name = "reply_to_sender")
    private String replyToSenderUsername;

    @Column(name = "sticker_code", length = 80)
    private String stickerCode;

    @Column(name = "sticker_url", length = 255)
    private String stickerUrl;

    @Column(name = "edited")
    private boolean edited;

    @Column(name = "edited_at")
    private LocalDateTime editedAt;

    @Column(name = "forwarded_from", length = 80)
    private String forwardedFrom;

    @Column(name = "pinned_at")
    private LocalDateTime pinnedAt;

    @Column(name = "pinned_by", length = 255)
    private String pinnedByUsername;

    @ElementCollection
    @CollectionTable(name = "group_message_deletions", joinColumns = @JoinColumn(name = "message_id"))
    @Column(name = "user_id")
    private Set<Long> deletedByUserIds = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private ChatGroup group;

    public GroupMessage() {
    }

    public Long getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public ChatGroup getGroup() {
        return group;
    }

    public void setGroup(ChatGroup group) {
        this.group = group;
    }

    public String getAttachmentFilename() {
        return attachmentFilename;
    }

    public void setAttachmentFilename(String attachmentFilename) {
        this.attachmentFilename = attachmentFilename;
    }

    public String getAttachmentType() {
        return attachmentType;
    }

    public void setAttachmentType(String attachmentType) {
        this.attachmentType = attachmentType;
    }

    public String getAttachmentOriginalName() {
        return attachmentOriginalName;
    }

    public void setAttachmentOriginalName(String attachmentOriginalName) {
        this.attachmentOriginalName = attachmentOriginalName;
    }

    public Long getAttachmentSize() {
        return attachmentSize;
    }

    public void setAttachmentSize(Long attachmentSize) {
        this.attachmentSize = attachmentSize;
    }

    public boolean hasAttachment() {
        return attachmentFilename != null && !attachmentFilename.isBlank();
    }

    public String getAudioUrl() { return audioUrl; }
    public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }

    public Long getAudioDurationMs() { return audioDurationMs; }
    public void setAudioDurationMs(Long audioDurationMs) { this.audioDurationMs = audioDurationMs; }

    public boolean hasAudio() { return audioUrl != null && !audioUrl.isBlank(); }

    public Long getReplyToMessageId() { return replyToMessageId; }
    public void setReplyToMessageId(Long replyToMessageId) { this.replyToMessageId = replyToMessageId; }

    public String getReplyToContent() { return replyToContent; }
    public void setReplyToContent(String replyToContent) { this.replyToContent = replyToContent; }

    public String getReplyToSenderUsername() { return replyToSenderUsername; }
    public void setReplyToSenderUsername(String replyToSenderUsername) { this.replyToSenderUsername = replyToSenderUsername; }

    public Set<Long> getDeletedByUserIds() { return deletedByUserIds; }
    public void setDeletedByUserIds(Set<Long> deletedByUserIds) { this.deletedByUserIds = deletedByUserIds; }

    public boolean isDeletedByUser(Long userId) { return deletedByUserIds.contains(userId); }
    public void addDeletion(Long userId) { deletedByUserIds.add(userId); }

    public String getStickerCode() { return stickerCode; }
    public void setStickerCode(String stickerCode) { this.stickerCode = stickerCode; }

    public String getStickerUrl() { return stickerUrl; }
    public void setStickerUrl(String stickerUrl) { this.stickerUrl = stickerUrl; }

    public boolean hasSticker() { return stickerUrl != null && !stickerUrl.isBlank(); }

    public boolean isEdited() { return edited; }
    public void setEdited(boolean edited) { this.edited = edited; }

    public LocalDateTime getEditedAt() { return editedAt; }
    public void setEditedAt(LocalDateTime editedAt) { this.editedAt = editedAt; }

    public String getForwardedFrom() { return forwardedFrom; }
    public void setForwardedFrom(String forwardedFrom) { this.forwardedFrom = forwardedFrom; }

    public LocalDateTime getPinnedAt() { return pinnedAt; }
    public void setPinnedAt(LocalDateTime pinnedAt) { this.pinnedAt = pinnedAt; }

    public String getPinnedByUsername() { return pinnedByUsername; }
    public void setPinnedByUsername(String pinnedByUsername) { this.pinnedByUsername = pinnedByUsername; }

    public boolean isPinned() { return pinnedAt != null; }
}
