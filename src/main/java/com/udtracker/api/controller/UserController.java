package com.udtracker.api.controller;

import com.udtracker.api.dto.LinkedAccountDto;
import com.udtracker.api.dto.UserProfileDto;
import com.udtracker.api.model.AppUser;
import com.udtracker.api.model.Player;
import com.udtracker.api.repository.AppUserRepository;
import com.udtracker.api.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.context.SecurityContextHolder;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class    UserController {

    private final AppUserRepository appUserRepository;
    private final PlayerRepository playerRepository;

    @GetMapping("/profile")
    @Transactional(readOnly = true)
    public UserProfileDto getProfile(Principal principal) {
        if (principal == null) throw new RuntimeException("Не авторизовано");

        AppUser user = appUserRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("Користувача не знайдено"));

        List<LinkedAccountDto> accounts = user.getGameAccounts().stream()
                .map(p -> new LinkedAccountDto(
                        p.getGameType(),
                        p.getSteamId(),
                        p.getNickname(),
                        p.getRiotId(),
                        p.getTagLine(),
                        p.getCurrentRank(),
                        p.getAverageRating(),
                        p.getPlayerCard()))
                .collect(Collectors.toList());

        return new UserProfileDto(user.getEmail(), user.getGlobalRating(), accounts);
    }

    @DeleteMapping("/link/{riotId}/{tagLine}")
    @Transactional
    public ResponseEntity<String> unlinkAccount(@PathVariable String riotId, @PathVariable String tagLine, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).body("Не авторизовано");

        AppUser user = appUserRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("Користувача не знайдено"));

        Player player = playerRepository.findByRiotIdAndTagLine(riotId, tagLine)
                .orElseThrow(() -> new RuntimeException("Ігровий акаунт не знайдено"));

        // Валідація власника
        if (player.getAppUser() == null || !player.getAppUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body("Цей акаунт вам не належить");
        }

        // Відв'язуємо
        player.setAppUser(null);
        playerRepository.save(player);

        return ResponseEntity.ok("Акаунт " + riotId + "#" + tagLine + " успішно відв'язано");
    }
    @GetMapping("/my-profile")
    public ResponseEntity<UserProfileDto> getMyProfile(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();

        AppUser user = appUserRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("Користувача не знайдено"));

        UserProfileDto dto = new UserProfileDto(); // Працює завдяки @NoArgsConstructor
        dto.setEmail(user.getEmail());
        dto.setGlobalRating(user.getGlobalRating());

        dto.setLinkedAccounts(user.getGameAccounts().stream()
                .map(p -> new LinkedAccountDto(
                        p.getGameType().name(),
                        p.getNickname(),
                        p.getCurrentRank(),
                        p.getAverageRating())) // Працює завдяки ручному конструктору
                .toList());

        return ResponseEntity.ok(dto);
    }
}