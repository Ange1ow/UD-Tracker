package com.udtracker.api.service;

import com.udtracker.api.model.AppUser;
import com.udtracker.api.model.GameType;
import com.udtracker.api.model.Player;
import com.udtracker.api.repository.AppUserRepository;
import com.udtracker.api.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BackgroundSyncService {
    private static final Logger log = LoggerFactory.getLogger(BackgroundSyncService.class);

    private final PlayerRepository playerRepository;
    private final AppUserRepository appUserRepository;
    private final ExternalApiService apiService;
    private final DotaApiService dotaApiService;

    @Scheduled(fixedDelay = 7200000)
    public void updateActiveProfilesAndGlobalRating() {
        log.info("Запуск фонового оновлення рангів...");

        List<Player> activePlayers = playerRepository.findByAppUserIsNotNull();
        for (Player p : activePlayers) {
            try {
                // Перевірка на тип гри (GameType)
                if (p.getGameType() == GameType.VALORANT) {
                    updateValorantPlayer(p);
                } else if (p.getGameType() == GameType.DOTA2) {
                    updateDotaPlayer(p);
                }
            } catch (Exception e) {
                log.error("Критична помилка обробки гравця ID {}: {}", p.getId(), e.getMessage());
            }
        }

        updateGlobalRatings();
        log.info("Фонове оновлення завершено.");
    }

    private void updateValorantPlayer(Player p) throws InterruptedException {
        if (p.getRegion() == null || p.getRiotId() == null) return;

        Thread.sleep(1500);
        var mmr = apiService.getPlayerMMR(p.getRegion(), p.getRiotId(), p.getTagLine());
        if (mmr != null && mmr.has("data")) {
            p.setCurrentRank(mmr.path("data").path("current_data").path("currenttierpatched").asText());
            playerRepository.save(p);
        }
    }

    private void updateDotaPlayer(Player p) {
        if (p.getSteamId() == null) return;

        String sid32 = dotaApiService.convertToSteamId32(p.getSteamId());
        var profile = dotaApiService.getPlayerProfile(sid32);
        if (profile != null && profile.has("rank_tier")) {
            p.setCurrentRank(profile.path("rank_tier").asText());
            playerRepository.save(p);
        }
    }

    private void updateGlobalRatings() {
        try {
            List<AppUser> users = appUserRepository.findAll();
            for (AppUser user : users) {
                double avg = user.getGameAccounts().stream()
                        .filter(a -> a.getAverageRating() != null && a.getAverageRating() > 0)
                        .mapToDouble(Player::getAverageRating)
                        .average().orElse(0.0);

                if (avg > 0) {
                    user.setGlobalRating(avg);
                    appUserRepository.save(user);
                }
            }
        } catch (Exception e) {
            log.error("Помилка підрахунку Global Rating: {}", e.getMessage());
        }
    }
}