package com.example.shadowvibe.Services;

import com.example.shadowvibe.Models.User;
import com.example.shadowvibe.Repositories.UserRepository;
import com.example.shadowvibe.enums.ThemePreference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(userRepository);
    }

    @Test
    void searchUsers_returnsEmptyForBlankQuery() {
        assertEquals(List.of(), service.searchUsers("   ", "alice"));
        assertEquals(List.of(), service.searchUsers(null, "alice"));
        verify(userRepository, never()).searchByUsername(any(), any(), any());
    }

    @Test
    void updateTheme_changesPreferenceAndSaves() {
        User user = new User("alice", "a@x.com", "p", null);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        User result = service.updateTheme("alice", ThemePreference.LIGHT);

        assertEquals(ThemePreference.LIGHT, result.getThemePreference());
        verify(userRepository).save(user);
    }

    @Test
    void updateTheme_throwsWhenUserMissing() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.updateTheme("ghost", ThemePreference.DARK));
    }

    @Test
    void updateProfile_rejectsNonImageContentType() {
        User user = new User("alice", "a@x.com", "p", null);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        MultipartFile bad = mock(MultipartFile.class);
        when(bad.getContentType()).thenReturn("text/plain");
        when(bad.isEmpty()).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> service.updateProfile("alice", null, bad));
        verify(userRepository, never()).save(any());
    }
}
