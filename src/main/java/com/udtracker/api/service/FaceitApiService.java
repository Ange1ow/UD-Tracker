// src/main/java/com/udtracker/api/service/FaceitApiService.java
package com.udtracker.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpStatusCodeException;

@Service
public class FaceitApiService {

    @Value("${faceit.api.key}")
    private String faceitApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + faceitApiKey);
        return headers;
    }

    public JsonNode getPlayerProfile(String nickname) {
        String url = "https://open.faceit.com/data/v4/players?nickname=" + nickname;
        try {
            return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(getHeaders()), JsonNode.class).getBody();
        } catch (HttpStatusCodeException e) {
            System.err.println("Faceit API HTTP Помилка (Профіль): " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            System.err.println("Faceit API Критична Помилка (Профіль): " + e.getMessage());
            return null;
        }
    }

    public JsonNode getPlayerStats(String playerId) {
        String url = "https://open.faceit.com/data/v4/players/" + playerId + "/stats/cs2";
        try {
            return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(getHeaders()), JsonNode.class).getBody();
        } catch (Exception e) {
            System.err.println("Помилка Faceit Stats: " + e.getMessage());
            return null;
        }
    }

    public JsonNode getMatchHistory(String playerId) {
        String url = "https://open.faceit.com/data/v4/players/" + playerId + "/history?game=cs2&offset=0&limit=10";
        try {
            return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(getHeaders()), JsonNode.class).getBody();
        } catch (Exception e) {
            System.err.println("Помилка Faceit History: " + e.getMessage());
            return null;
        }
    }

    public JsonNode getMatchStats(String matchId) {
        String url = "https://open.faceit.com/data/v4/matches/" + matchId + "/stats";
        try {
            return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(getHeaders()), JsonNode.class).getBody();
        } catch (Exception e) {
            System.err.println("Помилка Faceit Match Stats: " + e.getMessage());
            return null;
        }
    }
}