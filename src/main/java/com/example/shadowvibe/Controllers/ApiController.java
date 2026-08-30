package com.example.shadowvibe.Controllers;

import com.example.shadowvibe.DTO.ChatMessageDto;
import com.example.shadowvibe.DTO.ConversationPreviewDto;
import com.example.shadowvibe.Models.User;
import com.example.shadowvibe.Services.MessageService;
import com.example.shadowvibe.Services.PresenceService;
import com.example.shadowvibe.Services.UserService;
import com.example.shadowvibe.enums.UserRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    private static final String EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    private final UserService userService;
    private final MessageService messageService;
    private final PresenceService presenceService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final com.example.shadowvibe.Services.AttemptRateLimiter rateLimiter;

    public ApiController(UserService userService,
                         MessageService messageService,
                         PresenceService presenceService,
                         AuthenticationManager authenticationManager,
                         PasswordEncoder passwordEncoder,
                         com.example.shadowvibe.Services.AttemptRateLimiter rateLimiter) {
        this.userService = userService;
        this.messageService = messageService;
        this.presenceService = presenceService;
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String username = body.get("username");
        String password = body.get("password");
        if (username == null || password == null || username.isBlank() || password.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Введите логин и пароль"));
        }

        String rateKey = com.example.shadowvibe.Services.AttemptRateLimiter.KEY_LOGIN + username.trim().toLowerCase();
        if (rateLimiter.isBlocked(rateKey)) {
            return ResponseEntity.status(429).body(Map.of("error",
                    "Слишком много неудачных попыток входа. Повторите через "
                            + rateLimiter.remainingLockMinutes(rateKey) + " мин"));
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));
            SecurityContext context = SecurityContextHolder.getContext();
            context.setAuthentication(authentication);

            HttpSession session = request.getSession(true);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

            User user = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
            rateLimiter.recordSuccess(rateKey);
            return ResponseEntity.ok(toUserMap(user, user.getUsername()));
        } catch (org.springframework.security.core.AuthenticationException e) {
            rateLimiter.recordFailure(rateKey);
            return ResponseEntity.status(401).body(Map.of("error", "Неверный логин или пароль"));
        }
    }

    @PostMapping("/auth/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String username = body.get("username");
        String email = body.get("email");
        String password = body.get("password");

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Заполните все поля"));
        }
        if (password.length() < UserService.MIN_PASSWORD_LENGTH) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    "Пароль должен содержать минимум " + UserService.MIN_PASSWORD_LENGTH + " символов"));
        }
        if (userService.existsByUsername(username)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Пользователь с таким ником уже существует"));
        }
        if (email == null || !email.matches(EMAIL_REGEX)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Введите корректный email"));
        }
        email = email.trim().toLowerCase();
        if (userService.existsByEmail(email)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Этот email уже зарегистрирован"));
        }

        User user = new User(username, email, passwordEncoder.encode(password), UserRole.USER);
        userService.registerUser(user);

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        HttpSession session = request.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

        return ResponseEntity.ok(toUserMap(user, user.getUsername()));
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Не авторизован"));
        }
        User user = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        return ResponseEntity.ok(toUserMap(user, principal.getName()));
    }

    @PostMapping(value = "/profile/update", consumes = {"multipart/form-data"})
    public ResponseEntity<?> updateProfile(
            Principal principal,
            @RequestPart(value = "about", required = false) String about,
            @RequestPart(value = "avatar", required = false) MultipartFile avatar) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Не авторизовано"));
        }
        try {
            User updated = userService.updateProfile(principal.getName(), about, avatar);
            return ResponseEntity.ok(Map.of("success", true, "user", toUserMap(updated, principal.getName())));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Не удалось сохранить аватар"));
        }
    }


    @GetMapping("/conversations")
    public ResponseEntity<?> conversations(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Не авторизован"));
        }
        List<ConversationPreviewDto> conversations = messageService.getConversations(principal.getName());
        return ResponseEntity.ok(conversations);
    }

    @GetMapping("/conversations/{username}/messages")
    public ResponseEntity<?> chatHistory(@PathVariable String username, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Не авторизован"));
        }
        User receiver = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        List<ChatMessageDto> messages = messageService.getChatHistory(principal.getName(), username)
                .stream().map(messageService::toDto).toList();
        return ResponseEntity.ok(Map.of(
                "receiver", toUserMap(receiver, principal.getName()),
                "messages", messages
        ));
    }

    @GetMapping("/users/search")
    public ResponseEntity<?> searchUsers(@RequestParam(required = false) String q, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Не авторизован"));
        }
        List<Map<String, Object>> users = userService.searchUsers(q, principal.getName())
                .stream().map(u -> toUserMap(u, principal.getName())).toList();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/users/online")
    public ResponseEntity<?> onlinePartners(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Не авторизован"));
        }
        return ResponseEntity.ok(presenceService.getOnlinePartners(principal.getName()));
    }

    private Map<String, Object> toUserMap(User user, String viewerUsername) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("email", user.getEmail());
        map.put("avatarFilename", user.getAvatarFilename());
        map.put("about", user.getAbout());
        map.put("online", presenceService.isOnlineVisibleTo(viewerUsername, user.getUsername()));
        return map;
    }
}
