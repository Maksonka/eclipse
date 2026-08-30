package com.example.shadowvibe.Controllers;

import com.example.shadowvibe.Models.User;
import com.example.shadowvibe.Services.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/users")
    public String users(Principal principal, Model model) {
        User currentUser = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        List<User> users = userService.getAllUsers();

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("users", users);
        return "admin-users";
    }
}
