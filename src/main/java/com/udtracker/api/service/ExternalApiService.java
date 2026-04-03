package com.udtracker.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ExternalApiService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String API_KEY = "HDEV-cb5ebb99-7a7d-4198-a3cf-c612bb58a0fb";

    public JsonNode getPlayerData(String name, String tag) {
        String url = "https://api.henrikdev.xyz/valorant/v1/account/" + name + "/" + tag;

        try {
            // Створюємо заголовки з ключем
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", API_KEY);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Робимо запит з заголовками
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    JsonNode.class
            );

            System.out.println("Запит успішний!");
            return response.getBody();

        } catch (Exception e) {
            System.err.println("Деталі помилки API: " + e.getMessage());
            return null;
        }
    }
    public JsonNode getMatchHistory(String region, String name, String tag) {
        // Запит на останні 10 матчів
        String url = "https://api.henrikdev.xyz/valorant/v3/matches/" + region + "/" + name + "/" + tag + "?size=10";
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", API_KEY);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);
            return response.getBody();
        } catch (Exception e) {
            System.err.println("Помилка історії матчів: " + e.getMessage());
            return null;
        }
    }
    // Додай цей метод в ExternalApiService.java
    public JsonNode getPlayerMMR(String region, String name, String tag) {
        String url = "https://api.henrikdev.xyz/valorant/v2/mmr/" + region + "/" + name + "/" + tag;
        try {
            HttpHeaders headers = new HttpHeaders();
            // Якщо ти використовуєш API_KEY, він тут додасться
            if (API_KEY != null && !API_KEY.isEmpty()) {
                headers.set("Authorization", API_KEY);
            }
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);
            return response.getBody();
        } catch (Exception e) {
            System.err.println("Помилка отримання рангу: " + e.getMessage());
            return null; // Повертаємо null, якщо гравець ще не грав ранкед
        }
    }
}