package com.udtracker.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
public class DotaApiService {

    private static final Logger log = LoggerFactory.getLogger(DotaApiService.class);
    private final WebClient.Builder webClientBuilder;
    private final String BASE_URL = "https://api.opendota.com/api";

    /**
     * Отримання профілю гравця
     */
    public JsonNode getPlayerProfile(String steamId) {
        String accountId32 = convertToSteamId32(steamId);
        try {
            return webClientBuilder.baseUrl(BASE_URL).build()
                    .get()
                    .uri("/players/{id}", accountId32)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(); // .block() використовуємо лише у фонових задачах (BackgroundSyncService)
        } catch (Exception e) {
            log.error("Помилка OpenDota Profile для {}: {}", accountId32, e.getMessage());
            return null;
        }
    }

    /**
     * Отримання останніх матчів
     */
    public JsonNode getRecentMatches(String steamId) {
        String accountId32 = convertToSteamId32(steamId);
        try {
            return webClientBuilder.baseUrl(BASE_URL).build()
                    .get()
                    .uri("/players/{id}/recentMatches", accountId32)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
        } catch (Exception e) {
            log.error("Помилка OpenDota Matches для {}: {}", accountId32, e.getMessage());
            return null;
        }
    }

    /**
     * Конвертація SteamID64 у SteamID32 (необхідно для OpenDota API)
     */
    public String convertToSteamId32(String steamId) {
        if (steamId == null || steamId.isEmpty()) return "";
        try {
            long id64 = Long.parseLong(steamId);
            // Константа для конвертації: 76561197960265728
            if (id64 < 76561197960265728L) return steamId;
            return String.valueOf(id64 - 76561197960265728L);
        } catch (NumberFormatException e) {
            return steamId;
        }
    }
}