package com.udtracker.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class SteamApiService {

    @Value("${steam.api.key}")
    private String steamApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    // 1. Отримання базового профілю (Аватар, Нік, Статус)
    public JsonNode getPlayerSummaries(String steamId) {
        String url = String.format("http://api.steampowered.com/ISteamUser/GetPlayerSummaries/v0002/?key=%s&steamids=%s", steamApiKey, steamId);
        try {
            return restTemplate.getForObject(url, JsonNode.class);
        } catch (Exception e) {
            System.err.println("Помилка Steam API (Summaries): " + e.getMessage());
            return null;
        }
    }

    // 2. Отримання загальної статистики CS2 (AppID = 730)
    public JsonNode getUserStatsForCS2(String steamId) {
        String url = String.format("http://api.steampowered.com/ISteamUserStats/GetUserStatsForGame/v0002/?appid=730&key=%s&steamid=%s", steamApiKey, steamId);
        try {
            return restTemplate.getForObject(url, JsonNode.class);
        } catch (Exception e) {
            System.err.println("Помилка Steam API (CS2 Stats): " + e.getMessage());
            return null;
        }
    }

    // 3. Отримання історії матчів Dota 2 (AppID = 570)
    public JsonNode getDota2MatchHistory(String accountId) {
        // Увага: для деяких Dota API потрібен 32-bit Account ID замість 64-bit Steam ID
        String url = String.format("http://api.steampowered.com/IDOTA2Match_570/GetMatchHistory/v1/?key=%s&account_id=%s", steamApiKey, accountId);
        try {
            return restTemplate.getForObject(url, JsonNode.class);
        } catch (Exception e) {
            System.err.println("Помилка Steam API (Dota 2 Matches): " + e.getMessage());
            return null;
        }
    }

    // 4. Конвертація кастомного посилання (Vanity URL) у SteamID64
    public String resolveVanityUrl(String vanityName) {
        String url = String.format("http://api.steampowered.com/ISteamUser/ResolveVanityURL/v0001/?key=%s&vanityurl=%s", steamApiKey, vanityName);
        try {
            JsonNode response = restTemplate.getForObject(url, JsonNode.class);
            if (response != null && response.has("response") && response.path("response").path("success").asInt() == 1) {
                return response.path("response").path("steamid").asText();
            }
        } catch (Exception e) {
            System.err.println("Помилка Steam API (ResolveVanityURL): " + e.getMessage());
        }
        return null;
    }
}