package com.udtracker.api.service;

import com.udtracker.api.model.GameType;
import com.udtracker.api.model.Player;
import com.udtracker.api.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DotaStatsProvider implements GameStatsProvider {
    private static final Logger log = LoggerFactory.getLogger(DotaStatsProvider.class);
    private final DotaApiService dotaApiService;
    private final PlayerRepository playerRepository;

    @Override
    public GameType getGameType() {
        return GameType.DOTA2;
    }

    @Override
    public void updatePlayerStats(Player player) {
        try {
            if (player.getSteamId() == null) return;

            // 1. Конвертуємо SteamID64 у SteamID32 (як того вимагає твій сервіс)
            String accountId32 = dotaApiService.convertToSteamId32(player.getSteamId());

            // 2. Викликаємо правильний метод getPlayerProfile замість несуществуючого getPlayerStats
            var profile = dotaApiService.getPlayerProfile(accountId32);

            if (profile != null && profile.has("rank_tier") && !profile.get("rank_tier").isNull()) {
                // Оновлюємо ранг (напр. "80" для Immortal)
                player.setCurrentRank(profile.get("rank_tier").asText());
                playerRepository.save(player);
                log.info("Dota 2 stats updated for player: {}", accountId32);
            }
        } catch (Exception e) {
            log.error("Dota2 sync error for {}: {}", player.getSteamId(), e.getMessage());
        }
    }
}