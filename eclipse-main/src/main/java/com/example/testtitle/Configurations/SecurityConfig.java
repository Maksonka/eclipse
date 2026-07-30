package com.example.testtitle.Configurations;

import com.example.testtitle.Models.User;
import com.example.testtitle.Repositories.UserRepository;
import com.example.testtitle.Services.UserService;
import com.example.testtitle.enums.UserRole;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collections;
import java.util.Set;

@Configuration
public class SecurityConfig {

    private final UserRepository userRepository;

    public SecurityConfig(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // включить в продакшене
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/register", "/login", "/error", "/css/**", "/js/**", "/uploads/**").permitAll()
                        .requestMatchers("/ws/**").authenticated()
                        .requestMatchers("/account/**").hasAnyRole(UserRole.USER.name(), UserRole.ADMIN.name())
                        .anyRequest().authenticated())
                .formLogin(login -> login
                        .loginPage("/login").permitAll()
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .defaultSuccessUrl("/chat", true)
                        .failureUrl("/login?error"))
                .logout(logout -> logout
                        .permitAll()
                        .logoutSuccessUrl("/login"));
        return http.build();
    }

    //бин для шифрования пароля
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    //бин для того чтобы сообщить спрингу откуда брать данные о юзере
    @Bean
    public UserDetailsService userDetailsService() {
        return new UserDetailsService() {
            @Override
            public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
                User user = userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("Пользователь с именем " + username + " не найден"));
                Set<SimpleGrantedAuthority> roles = Collections.singleton(user.getRole().toAuthority());
                return new org.springframework.security.core.userdetails.User(user.getUsername(), user.getPassword(), roles);
            }
        };
    }


}