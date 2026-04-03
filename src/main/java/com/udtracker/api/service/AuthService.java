package com.udtracker.api.service;

import com.udtracker.api.dto.AuthRequest;
import com.udtracker.api.model.AppUser;
import com.udtracker.api.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public String register(AuthRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Користувач з таким email вже існує");
        }

        AppUser user = new AppUser();
        user.setEmail(request.getEmail());
        // Хешуємо пароль перед збереженням
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        userRepository.save(user);
        return "Реєстрація успішна";
    }
    public String login(AuthRequest request) {
        AppUser user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Користувача не знайдено"));

        // Перевіряємо, чи збігається введений пароль з хешем у базі
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Невірний пароль");
        }

        // Якщо все ок - генеруємо токен
        return jwtService.generateToken(user.getEmail());
    }
}