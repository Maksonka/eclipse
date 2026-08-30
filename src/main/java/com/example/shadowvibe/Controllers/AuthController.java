package com.example.shadowvibe.Controllers;

import com.example.shadowvibe.Models.User;
import com.example.shadowvibe.Repositories.UserRepository;
import com.example.shadowvibe.Services.UserService;

import com.example.shadowvibe.enums.UserRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    private static final String EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public AuthController(UserService userService, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager) {
        this.userService = userService;


        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }


    @GetMapping("/")
    public String home(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof UserDetails) {
            return "redirect:/chat";
        }
        return "home";
    }

    @GetMapping("/register")
    public String registerForm() {
        return "register";
    }

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error, Model model) {
        if (error != null) {
            model.addAttribute("loginError", true);
        }
        return "login";
    }

    @GetMapping("/account")
    public String accountRedirect() {
        return "redirect:/chat";
    }

    @GetMapping("/error")
    public String error() {
        return "error";
    }

    @PostMapping("/register")
    public String registerUser(@RequestParam String username,
                               @RequestParam String email,
                               @RequestParam String password,
                               HttpServletRequest request,
                               Model model) {
        model.addAttribute("enteredUsername", username);
        model.addAttribute("enteredEmail", email);

        if (userService.existsByUsername(username)) {
            model.addAttribute("registerError", "Пользователь с таким ником уже существует");
            return "register";
        }

        if (password == null || password.length() < UserService.MIN_PASSWORD_LENGTH) {
            model.addAttribute("registerError",
                    "Пароль должен содержать минимум " + UserService.MIN_PASSWORD_LENGTH + " символов");
            return "register";
        }

        if (email == null || !email.matches(EMAIL_REGEX)) {
            model.addAttribute("registerError", "Введите корректный email");
            return "register";
        }

        email = email.trim().toLowerCase();

        if (userService.existsByEmail(email)) {
            model.addAttribute("registerError", "Этот email уже зарегистрирован");
            return "register";
        }

        String encodedPassword = passwordEncoder.encode(password);        User user = new User(username, email, encodedPassword, UserRole.USER);
        userService.registerUser(user);

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        HttpSession session = request.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

        return "redirect:/chat";
    }
}