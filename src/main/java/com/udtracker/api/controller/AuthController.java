package com.udtracker.api.controller;

import com.udtracker.api.dto.AuthRequest;
import com.udtracker.api.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody AuthRequest request) {
        // @Valid автоматично перевірить email та довжину пароля з нашого DTO
        String result = authService.register(request);
        return ResponseEntity.ok(result);
    }
    @PostMapping("/login")
    public ResponseEntity<String> login(@Valid @RequestBody AuthRequest request) {
        String token = authService.login(request);
        return ResponseEntity.ok(token); // Повертаємо згенерований JWT токен
    }
}