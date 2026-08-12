package com.example.shadowvibe.Services;

import com.example.shadowvibe.DTO.StickerPackDto;
import com.example.shadowvibe.Models.StickerPack;
import com.example.shadowvibe.Models.User;
import com.example.shadowvibe.Repositories.StickerPackRepository;
import com.example.shadowvibe.Repositories.StickerRepository;
import com.example.shadowvibe.Repositories.UserRepository;
import com.example.shadowvibe.Repositories.UserStickerPackRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StickerServiceTest {

    @Mock
    private StickerRepository stickerRepository;
    @Mock
    private StickerPackRepository stickerPackRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserStickerPackRepository userStickerPackRepository;

    private StickerService service;

    @BeforeEach
    void setUp() {
        service = new StickerService(stickerRepository, stickerPackRepository, userRepository, userStickerPackRepository);
    }

    @Test
    void createPack_rejectsBlankName() {
        assertThrows(IllegalArgumentException.class, () -> service.createPack("alice", "   "));
        assertThrows(IllegalArgumentException.class, () -> service.createPack("alice", null));
    }

    @Test
    void createPack_rejectsTooLongName() {
        assertThrows(IllegalArgumentException.class, () -> service.createPack("alice", "x".repeat(61)));
    }

    @Test
    void createPack_rejectsDuplicateName() {
        when(stickerPackRepository.findByNameIgnoreCase("Kitty")).thenReturn(Optional.of(new StickerPack("Kitty", "bob")));
        assertThrows(IllegalArgumentException.class, () -> service.createPack("alice", "  Kitty  "));
        verify(stickerPackRepository, never()).save(any());
    }

    @Test
    void createPack_createsAndReturnsDto() {
        when(stickerPackRepository.findByNameIgnoreCase("Kitty")).thenReturn(Optional.empty());
        when(stickerPackRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        User alice = mock(User.class);
        when(alice.getId()).thenReturn(5L);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(userStickerPackRepository.findPackIdsByUserId(5L)).thenReturn(Set.of());

        StickerPackDto dto = service.createPack("alice", "Kitty");

        assertEquals("Kitty", dto.getName());
        assertTrue(dto.isMine());
        assertTrue(dto.isAdded());
    }

    @Test
    void subscribePack_returnsForSystemPackWithoutSaving() {
        StickerPack system = new StickerPack("Default", null);
        when(stickerPackRepository.findById(1L)).thenReturn(Optional.of(system));

        StickerPackDto dto = service.subscribePack("alice", 1L);

        assertTrue(dto.isAdded());
        verify(userStickerPackRepository, never()).save(any());
    }

    @Test
    void subscribePack_savesMembershipForForeignPack() {
        StickerPack foreign = new StickerPack("Kitty", "bob");
        when(stickerPackRepository.findById(2L)).thenReturn(Optional.of(foreign));
        User alice = new User("alice", "alice@x.com", "p", null);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(userStickerPackRepository.existsByUserIdAndPackId(any(), any())).thenReturn(false);

        service.subscribePack("alice", 2L);

        verify(userStickerPackRepository).save(any());
    }

    @Test
    void subscribePack_doesNotDuplicateMembership() {
        StickerPack foreign = new StickerPack("Kitty", "bob");
        when(stickerPackRepository.findById(2L)).thenReturn(Optional.of(foreign));
        User alice = new User("alice", "alice@x.com", "p", null);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(userStickerPackRepository.existsByUserIdAndPackId(any(), any())).thenReturn(true);

        service.subscribePack("alice", 2L);

        verify(userStickerPackRepository, never()).save(any());
    }

    @Test
    void addStickers_rejectsNonOwner() {
        StickerPack pack = new StickerPack("Kitty", "bob");
        pack.setId(1L);
        when(stickerPackRepository.findById(1L)).thenReturn(Optional.of(pack));

        assertThrows(IllegalArgumentException.class,
                () -> service.addStickers("alice", 1L, new MultipartFile[]{mockFile()}));
    }

    @Test
    void addStickers_rejectsTooManyFiles() {
        StickerPack pack = new StickerPack("Kitty", "alice");
        pack.setId(1L);
        when(stickerPackRepository.findById(1L)).thenReturn(Optional.of(pack));

        MultipartFile[] files = new MultipartFile[31];
        for (int i = 0; i < files.length; i++) {
            files[i] = mockFile();
        }
        assertThrows(IllegalArgumentException.class, () -> service.addStickers("alice", 1L, files));
    }

    @Test
    void addStickers_rejectsWrongContentType() {
        StickerPack pack = new StickerPack("Kitty", "alice");
        pack.setId(1L);
        when(stickerPackRepository.findById(1L)).thenReturn(Optional.of(pack));
        when(stickerRepository.countByPackId(1L)).thenReturn(0L);
        MultipartFile file = mockFile();
        when(file.getContentType()).thenReturn("application/octet-stream");
        when(file.getSize()).thenReturn(100L);

        assertThrows(IllegalArgumentException.class, () -> service.addStickers("alice", 1L, new MultipartFile[]{file}));
    }

    @Test
    void listPacks_filtersToSystemMineAndSubscribed() {
        User alice = mock(User.class);
        when(alice.getId()).thenReturn(7L);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(userStickerPackRepository.findPackIdsByUserId(7L)).thenReturn(Set.of(30L));

        StickerPack system = new StickerPack("System", null);
        system.setId(1L);
        StickerPack mine = new StickerPack("Mine", "alice");
        mine.setId(2L);
        StickerPack foreign = new StickerPack("Foreign", "bob");
        foreign.setId(3L);
        StickerPack subscribed = new StickerPack("Sub", "carol");
        subscribed.setId(30L);
        when(stickerPackRepository.findAllByOrderByCreatedAtAsc()).thenReturn(List.of(system, mine, foreign, subscribed));

        List<StickerPackDto> dtos = service.listPacks("alice");

        assertEquals(3, dtos.size());
        assertEquals(List.of("System", "Mine", "Sub"), dtos.stream().map(StickerPackDto::getName).toList());
    }

    private MultipartFile mockFile() {
        return org.mockito.Mockito.mock(MultipartFile.class);
    }
}
