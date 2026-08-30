package com.example.shadowvibe.enums;

public enum ThemePreference {
    DARK,
    LIGHT,
    AURORA,
    SUNSET;

    public boolean isPremium() {
        return this != DARK && this != LIGHT;
    }
}
