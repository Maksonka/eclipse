package com.example.shadowvibe.Models;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "group_invites",
       uniqueConstraints = @UniqueConstraint(columnNames = {"group_id", "invited_user_id"}))
public class GroupInvite {

    public enum Status { PENDING, ACCEPTED, DECLINED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private ChatGroup group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invited_by_id", nullable = false)
    private User invitedBy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invited_user_id", nullable = false)
    private User invitedUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public GroupInvite() {}

    public GroupInvite(ChatGroup group, User invitedBy, User invitedUser) {
        this.group = group;
        this.invitedBy = invitedBy;
        this.invitedUser = invitedUser;
    }

    public Long getId() { return id; }
    public ChatGroup getGroup() { return group; }
    public User getInvitedBy() { return invitedBy; }
    public User getInvitedUser() { return invitedUser; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
