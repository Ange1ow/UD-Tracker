package com.udtracker.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.udtracker.api.model.MatchData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DotaApiService {

    private final WebClient.Builder webClientBuilder;
    private static final String BASE_URL = "https://api.opendota.com/api";

    public JsonNode getPlayerProfile(String steamId) {
        String accountId32 = convertToSteamId32(steamId);
        try {
            return webClientBuilder.baseUrl(BASE_URL).build()
                    .get()
                    .uri("/players/{id}", accountId32)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
        } catch (Exception e) {
            log.error("Помилка OpenDota Profile для {}: {}", accountId32, e.getMessage());
            return null;
        }
    }

    public List<MatchData> getRecentMatches(String steamId) {
        String accountId32 = convertToSteamId32(steamId);
        try {
            JsonNode recentMatches = webClientBuilder.baseUrl(BASE_URL).build()
                    .get()
                    .uri("/players/{id}/recentMatches", accountId32)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            List<MatchData> matches = new ArrayList<>();
            if (recentMatches == null || !recentMatches.isArray()) return matches;

            int limit = Math.min(recentMatches.size(), 10);

            for (int i = 0; i < limit; i++) {
                JsonNode node = recentMatches.get(i);
                String matchId = node.path("match_id").asText();

                MatchData data = new MatchData();
                data.setMatchId(matchId);
                data.setMode("Matchmaking");

                // Базові метрики, які є ЗАВЖДИ
                data.setKills(node.path("kills").asInt());
                data.setDeaths(node.path("deaths").asInt());
                data.setAssists(node.path("assists").asInt());

                int gpm = node.has("gold_per_min") ? node.get("gold_per_min").asInt() : 0;
                int xpm = node.has("xp_per_min") ? node.get("xp_per_min").asInt() : 0;
                int heroDamage = node.has("hero_damage") ? node.get("hero_damage").asInt() : 0;

                if (gpm == 0) {
                    log.info("Поверхнева стата порожня. Тягнемо глибокий парсинг для матчу: {}", matchId);
                    JsonNode deepMatch = getMatchDetails(matchId);

                    if (deepMatch != null && deepMatch.has("players")) {
                        for (JsonNode player : deepMatch.get("players")) {
                            if (player.path("account_id").asText().equals(accountId32)) {
                                // Перезаписуємо розширеними даними
                                data.setGpm(player.path("gold_per_min").asInt());
                                data.setXpm(player.path("xp_per_min").asInt());
                                data.setHeroDamage(player.path("hero_damage").asInt());
                                break;
                            }
                        }
                    }
                } else {
                    data.setGpm(gpm);
                    data.setXpm(xpm);
                    data.setHeroDamage(heroDamage);
                }

                matches.add(data);
            }
            return matches;
        } catch (Exception e) {
            log.error("Помилка OpenDota Matches для {}: {}", accountId32, e.getMessage());
            return List.of();
        }
    }

    public JsonNode getMatchDetails(String matchId) {
        try {
            return webClientBuilder.baseUrl(BASE_URL).build()
                    .get()
                    .uri("/matches/{match_id}", matchId)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
        } catch (Exception e) {
            log.error("Помилка OpenDota Match Details для {}: {}", matchId, e.getMessage());
            return null;
        }
    }

    public String convertToSteamId32(String steamId) {
        if (steamId == null || steamId.isEmpty()) return "";
        try {
            long id64 = Long.parseLong(steamId);
            if (id64 < 76561197960265728L) return steamId;
            return String.valueOf(id64 - 76561197960265728L);
        } catch (NumberFormatException e) {
            return steamId;
        }
    }
}