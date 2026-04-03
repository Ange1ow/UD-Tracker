package com.udtracker.api.controller;

import com.udtracker.api.model.MatchData;
import com.udtracker.api.model.Player;
import com.udtracker.api.repository.MatchRepository;
import com.udtracker.api.repository.PlayerRepository;
import com.udtracker.api.service.ExternalApiService;
import com.udtracker.api.service.RatingService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.security.Principal;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import com.udtracker.api.repository.AppUserRepository;
import com.udtracker.api.model.AppUser;

@RestController
@RequestMapping("/api/v1/players")
@RequiredArgsConstructor // Створює конструктор для всіх final полів
public class PlayerController {

    private final ExternalApiService apiService;
    private final RatingService ratingService;
    private final PlayerRepository playerRepository;
    private final MatchRepository matchRepository; // Робимо final
    private final AppUserRepository appUserRepository;

    @GetMapping("/sync/{name}/{tag}")
    public String syncPlayer(@PathVariable String name, @PathVariable String tag) {
        // 1. Отримуємо базовий профіль (Рівень, Картка, Регіон)
        JsonNode response = apiService.getPlayerData(name, tag);

        if (response != null && response.has("data")) {
            JsonNode data = response.path("data");

            Player player = playerRepository.findByRiotIdAndTagLine(name, tag)
                    .orElse(new Player());

            player.setRiotId(name);
            player.setTagLine(tag);
            player.setAccountLevel(data.path("account_level").asInt());

            String region = data.path("region").asText();
            player.setRegion(region);
            player.setPlayerCard(data.path("card").path("wide").asText());

            // 2. НОВИЙ КРОК: Отримуємо ранг окремим запитом
            JsonNode mmrResponse = apiService.getPlayerMMR(region, name, tag);

            if (mmrResponse != null && mmrResponse.has("data")) {
                // Шлях до рангу у v2 MMR API
                String rank = mmrResponse.path("data").path("current_data").path("currenttierpatched").asText();
                player.setCurrentRank(rank != null && !rank.isEmpty() && !rank.equals("null") ? rank : "Unranked");
            } else {
                player.setCurrentRank("Unranked");
            }

            playerRepository.save(player);

            return String.format("Синхронізовано! Гравець: %s#%s, Ранг: %s", name, tag, player.getCurrentRank());
        }
        return "Помилка: дані не отримано.";
    }

    @Transactional
    @GetMapping("/matches/{name}/{tag}")
    public String syncMatches(@PathVariable String name, @PathVariable String tag) {
        Player player = playerRepository.findByRiotIdAndTagLine(name, tag)
                .orElseThrow(() -> new RuntimeException("Спочатку синхронізуйте профіль!"));

        // 1. ЗАХИСТ ВІД СПАМУ API (КЕШУВАННЯ НА 10 ХВИЛИН)
        if (player.getLastUpdated() != null &&
                java.time.temporal.ChronoUnit.MINUTES.between(player.getLastUpdated(), java.time.LocalDateTime.now()) < 10) {
            return "Дані актуальні. Завантажено з локальної бази (Кеш).";
        }

        // 2. Якщо пройшло 10 хв - робимо запит до Henrik API
        JsonNode response = apiService.getMatchHistory(player.getRegion(), name, tag);

        if (response != null && response.has("data")) {
            matchRepository.deleteByPlayerId(player.getId());
            JsonNode matches = response.path("data");

            double totalKills = 0, totalDeaths = 0, totalHs = 0, totalAdr = 0, totalRating = 0;
            int count = 0;

            for (JsonNode match : matches) {
                String matchId = match.path("metadata").path("matchid").asText();
                String mode = match.path("metadata").path("mode").asText();
                String mapName = match.path("metadata").path("map").asText();
                JsonNode players = match.path("players").path("all_players");

                for (JsonNode p : players) {
                    if (p.path("name").asText().equalsIgnoreCase(name) && p.path("tag").asText().equalsIgnoreCase(tag)) {
                        MatchData matchData = new MatchData();
                        matchData.setMatchId(matchId);
                        matchData.setAgent(p.path("character").asText());

                        JsonNode stats = p.path("stats");
                        int kills = stats.path("kills").asInt();
                        int deaths = stats.path("deaths").asInt();
                        int assists = stats.path("assists").asInt();
                        int rounds = match.path("metadata").path("rounds_played").asInt();
                        int damage = p.has("damage_made") ? p.path("damage_made").asInt() : 0;
                        int adr = rounds > 0 ? damage / rounds : 0;

                        int headshots = stats.path("headshots").asInt();
                        int totalShots = headshots + stats.path("bodyshots").asInt() + stats.path("legshots").asInt();
                        int hsPercent = totalShots > 0 ? (int) Math.round((double) headshots / totalShots * 100.0) : 0;

                        matchData.setKills(kills); matchData.setDeaths(deaths); matchData.setAssists(assists);
                        matchData.setAdr(adr); matchData.setHsPercent(hsPercent); matchData.setMode(mode);

                        double finalRating;
                        if (mode.equalsIgnoreCase("Deathmatch") || mode.equalsIgnoreCase("Team Deathmatch")) {
                            finalRating = deaths > 0 ? (double) kills / deaths : kills;
                            matchData.setMap(mapName + " (DM)");
                        } else {
                            finalRating = ratingService.calculateRating(kills, deaths, adr, rounds);
                            matchData.setMap(mapName);
                        }
                        matchData.setRating21(finalRating); matchData.setPlayer(player);
                        matchRepository.save(matchData);

                        // Збираємо суми для середньої статистики
                        totalKills += kills; totalDeaths += deaths; totalHs += hsPercent;
                        totalAdr += adr; totalRating += finalRating;
                        count++;
                        break;
                    }
                }
            }

            // 3. ЗБЕРІГАЄМО ВСЮ СТАТИСТИКУ ПРЯМО В ПРОФІЛЬ ГРАВЦЯ В БД
            if (count > 0) {
                player.setAverageKd(totalDeaths == 0 ? totalKills : totalKills / totalDeaths);
                player.setAverageHs((int) (totalHs / count));
                player.setAverageAdr((int) (totalAdr / count));
                player.setAverageRating(totalRating / count);
                player.setLastUpdated(java.time.LocalDateTime.now()); // СТАВИМО ШТАМП ЧАСУ!
                playerRepository.save(player);
            }

            return "Успішно оновлено! API Хенріка опитано.";
        }
        return "Не вдалося отримати історію матчів.";
    }
    @GetMapping("/stats/{name}/{tag}")
    public String getPlayerStats(@PathVariable String name, @PathVariable String tag) {
        Player player = playerRepository.findByRiotIdAndTagLine(name, tag)
                .orElseThrow(() -> new RuntimeException("Гравця не знайдено. Спочатку синхронізуйте матчі!"));

        // Беремо рейтинг прямо з профілю, який ми щойно закешували!
        Double avgRating = player.getAverageRating();

        if (avgRating == null || avgRating == 0.0) {
            return "Для гравця " + name + " ще немає даних про матчі.";
        }

        String skillLevel;
        if (avgRating > 1.2) skillLevel = "PRO (Carry)";
        else if (avgRating > 1.0) skillLevel = "Solid Player";
        else if (avgRating > 0.8) skillLevel = "Average";
        else skillLevel = "Needs Practice";

        return String.format(
                "Аналіз останніх матчів для %s#%s:\n" +
                        "- Середній Rating 2.1: %.2f\n" +
                        "- Ваш поточний рівень: %s",
                name, tag, avgRating, skillLevel
        );
    }
    @GetMapping("/history/{name}/{tag}")
    public List<MatchData> getMatchHistory(@PathVariable String name, @PathVariable String tag) {
        Player player = playerRepository.findByRiotIdAndTagLine(name, tag)
                .orElseThrow(() -> new RuntimeException("Гравця не знайдено"));

        return matchRepository.findByPlayerId(player.getId());
    }
    @GetMapping("/profile/{name}/{tag}")
    public Player getProfile(@PathVariable String name, @PathVariable String tag) {
        return playerRepository.findByRiotIdAndTagLine(name, tag)
                .orElseThrow(() -> new RuntimeException("Гравця не знайдено"));
    }
    @GetMapping("/leaderboard")
    public List<Player> getLeaderboard() {
        return playerRepository.findTop10ByOrderByAverageRatingDesc();
    }
    @PostMapping("/link/{name}/{tag}")
    @SecurityRequirement(name = "Bearer Authentication") // Вказуємо Swagger, що тут потрібен токен
    public String linkAccount(@PathVariable String name, @PathVariable String tag, Principal principal) {
        // Якщо токена немає, сюди навіть не дійде (зупинить JwtFilter), але перевірка не завадить
        if (principal == null) {
            return "Помилка: Ви не авторизовані!";
        }

        // 1. Знаходимо поточного юзера в БД (його email лежить у principal)
        AppUser currentUser = appUserRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("Користувача не знайдено"));

        // 2. Шукаємо ігровий профіль (або створюємо новий порожній, якщо ще не синхронізували)
        Player player = playerRepository.findByRiotIdAndTagLine(name, tag)
                .orElse(new Player());

        // 3. Зв'язуємо сутності
        player.setRiotId(name);
        player.setTagLine(tag);
        player.setAppUser(currentUser); // ПРИВ'ЯЗКА!

        playerRepository.save(player);

        return String.format("Ігровий акаунт %s#%s успішно прив'язано до email: %s", name, tag, currentUser.getEmail());
    }
}