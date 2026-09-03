package com.example.shadowvibe.Models;

import jakarta.persistence.*;

@Entity
@Table(name = "ghost_exceptions", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "exception_user_id"})
})
public class GhostException {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exception_user_id", nullable = false)
    private User exceptionUser;

    @Column(name = "show_activity", nullable = false)
    private boolean showActivity = false;

    public GhostException() {}

    public GhostException(User user, User exceptionUser, boolean showActivity) {
        this.user = user;
        this.exceptionUser = exceptionUser;
        this.showActivity = showActivity;
    }

    public Long getId() { return id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public User getExceptionUser() { return exceptionUser; }
    public void setExceptionUser(User exceptionUser) { this.exceptionUser = exceptionUser; }

    public boolean isShowActivity() { return showActivity; }
    public void setShowActivity(boolean showActivity) { this.showActivity = showActivity; }
}
