package com.example.shadowvibe.Models;

import com.example.shadowvibe.enums.UserRole;
import com.example.shadowvibe.enums.ThemePreference;
import com.example.shadowvibe.enums.MessagesFrom;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @Column(name = "username", nullable = false)
    private String username;
    @Column(name = "email", unique = true)
    private String email;
    @Column(name = "password", nullable = false)
    private String password;
    @Column(name = "role", nullable = false)
    private UserRole role;

    @Column(name = "about", length = 500)
    private String about;

    @Column(name = "avatar_filename")
    private String avatarFilename;

    @Enumerated(EnumType.STRING)
    @Column(name = "theme_preference", nullable = false)
    private ThemePreference themePreference = ThemePreference.DARK;

    @Column(name = "hide_online_status", nullable = false)
    private boolean hideOnlineStatus = false;

    @Column(name = "searchable", nullable = false)
    private boolean searchable = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "messages_from", nullable = false)
    private MessagesFrom messagesFrom = MessagesFrom.ALL;

    @Column(name = "premium_until")
    private LocalDateTime premiumUntil;

    @Column(name = "premium_trial_used", nullable = false)
    private boolean premiumTrialUsed = false;

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public User(String username, String email, String password, UserRole role) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    public User() {
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getAbout() {
        return about;
    }

    public void setAbout(String about) {
        this.about = about;
    }

    public String getAvatarFilename() {
        return avatarFilename;
    }

    public void setAvatarFilename(String avatarFilename) {
        this.avatarFilename = avatarFilename;
    }

    public boolean hasAvatar() {
        return avatarFilename != null && !avatarFilename.isBlank();
    }

    public ThemePreference getThemePreference() {
        return themePreference;
    }

    public void setThemePreference(ThemePreference themePreference) {
        this.themePreference = themePreference;
    }

    public boolean isHideOnlineStatus() {
        return hideOnlineStatus;
    }

    public void setHideOnlineStatus(boolean hideOnlineStatus) {
        this.hideOnlineStatus = hideOnlineStatus;
    }

    public boolean isSearchable() {
        return searchable;
    }

    public void setSearchable(boolean searchable) {
        this.searchable = searchable;
    }

    public MessagesFrom getMessagesFrom() {
        return messagesFrom;
    }

    public void setMessagesFrom(MessagesFrom messagesFrom) {
        this.messagesFrom = messagesFrom;
    }

    public LocalDateTime getPremiumUntil() {
        return premiumUntil;
    }

    public void setPremiumUntil(LocalDateTime premiumUntil) {
        this.premiumUntil = premiumUntil;
    }

    public boolean isPremiumTrialUsed() {
        return premiumTrialUsed;
    }

    public void setPremiumTrialUsed(boolean premiumTrialUsed) {
        this.premiumTrialUsed = premiumTrialUsed;
    }

    public boolean isPremium() {
        return premiumUntil != null && premiumUntil.isAfter(java.time.LocalDateTime.now());
    }
}
