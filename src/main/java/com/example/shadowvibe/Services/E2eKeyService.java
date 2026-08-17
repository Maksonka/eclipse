package com.example.shadowvibe.Services;

import com.example.shadowvibe.Models.User;
import com.example.shadowvibe.Models.UserKey;
import com.example.shadowvibe.Repositories.UserKeyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class E2eKeyService {

    private final UserKeyRepository userKeyRepository;
    private final UserService userService;

    public E2eKeyService(UserKeyRepository userKeyRepository, UserService userService) {
        this.userKeyRepository = userKeyRepository;
        this.userService = userService;
    }

    @Transactional
    public boolean saveIdentityKey(String username, String publicKey) {
        if (publicKey == null || publicKey.isBlank() || publicKey.length() > 128) {
            return false;
        }
        User user = userService.findByUsername(username).orElse(null);
        if (user == null) {
            return false;
        }
        UserKey existing = userKeyRepository.findByUserId(user.getId()).orElse(null);
        if (existing == null) {
            userKeyRepository.save(new UserKey(user.getId(), publicKey.trim()));
        } else {
            existing.setIdentityPublicKey(publicKey.trim());
            userKeyRepository.save(existing);
        }
        return true;
    }

    @Transactional(readOnly = true)
    public Optional<String> findIdentityPublicKey(String username) {
        User user = userService.findByUsername(username).orElse(null);
        if (user == null) {
            return Optional.empty();
        }
        return userKeyRepository.findByUserId(user.getId()).map(UserKey::getIdentityPublicKey);
    }

    @Transactional(readOnly = true)
    public boolean hasKey(String username) {
        return findIdentityPublicKey(username).isPresent();
    }
}
