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

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final AppUserRepository appUserRepository;
    private final PlayerRepository playerRepository;

    @GetMapping("/profile")
    @Transactional(readOnly = true)
    public UserProfileDto getProfile(Principal principal) {
        AppUser user = getAuthenticatedUser(principal);

        // Обходимо LAZY loading - робимо явний запит в БД
        List<Player> gameAccounts = playerRepository.findAllByAppUser(user);

        List<LinkedAccountDto> accounts = gameAccounts.stream()
                .map(p -> new LinkedAccountDto(
                        p.getId(),
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

    @GetMapping("/my-profile")
    @Transactional(readOnly = true)
    public ResponseEntity<UserProfileDto> getMyProfile(Principal principal) {
        AppUser user = getAuthenticatedUser(principal);

        List<Player> gameAccounts = playerRepository.findAllByAppUser(user);

        UserProfileDto dto = new UserProfileDto();
        dto.setEmail(user.getEmail());
        dto.setGlobalRating(user.getGlobalRating());

        // ВАЖЛИВО: Якщо на фронтенді ти чекаєш data.accounts, зміни setLinkedAccounts на setAccounts (і в DTO також)
        dto.setLinkedAccounts(gameAccounts.stream()
                .map(p -> new LinkedAccountDto(
                        p.getId(),
                        p.getGameType(),
                        p.getNickname() != null ? p.getNickname() : (p.getRiotId() + "#" + p.getTagLine()),
                        p.getCurrentRank(),
                        p.getAverageRating()))
                .toList());

        return ResponseEntity.ok(dto);
    }

    // Універсальне відв'язування для будь-якої гри за ID гравця
    @DeleteMapping("/link/{playerId}")
    @Transactional
    public ResponseEntity<String> unlinkAccount(@PathVariable Long playerId, Principal principal) {
        AppUser user = getAuthenticatedUser(principal);

        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Ігровий акаунт не знайдено"));

        // Валідація власника
        if (player.getAppUser() == null || !player.getAppUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body("Цей акаунт вам не належить");
        }

        // Відв'язуємо
        player.setAppUser(null);
        playerRepository.save(player);

        return ResponseEntity.ok("Акаунт успішно відв'язано");
    }

    // DRY: Допоміжний метод для отримання користувача
    private AppUser getAuthenticatedUser(Principal principal) {
        if (principal == null) throw new RuntimeException("Не авторизовано");
        return appUserRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("Користувача не знайдено"));
    }
}