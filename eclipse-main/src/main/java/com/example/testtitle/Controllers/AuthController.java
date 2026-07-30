package com.example.testtitle.Controllers;

import com.example.testtitle.Models.User;
import com.example.testtitle.Repositories.UserRepository;
import com.example.testtitle.Services.UserService;

import com.example.testtitle.enums.UserRole;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;


        this.passwordEncoder = passwordEncoder;
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
                               @RequestParam String password,
                               Model model) {
        if (userService.existsByUsername(username)) {
            model.addAttribute("registerError", "Пользователь с таким ником уже существует");
            return "register";
        }

        String encodedPassword = passwordEncoder.encode(password);
        User user = new User(username, encodedPassword, UserRole.USER);
        userService.registerUser(user);
        return "redirect:/login";
    }
}