package com.udtracker.api.controller;

import com.udtracker.api.model.AppUser;
import com.udtracker.api.model.GameType;
import com.udtracker.api.model.MatchData;
import com.udtracker.api.model.Player;
import com.udtracker.api.repository.AppUserRepository;
import com.udtracker.api.repository.MatchRepository;
import com.udtracker.api.repository.PlayerRepository;
import com.udtracker.api.service.DotaApiService;
import com.udtracker.api.service.ExternalApiService;
import com.udtracker.api.service.FaceitApiService;
import com.udtracker.api.service.RatingService;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder; // ДОДАНО
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.context.SecurityContextHolder; // Обов'язково для JWT
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/players")
@RequiredArgsConstructor
public class PlayerController {

    private final ExternalApiService apiService;
    private final RatingService ratingService;
    private final PlayerRepository playerRepository;
    private final MatchRepository matchRepository;
    private final AppUserRepository appUserRepository;
    private final FaceitApiService faceitApiService;
    private final DotaApiService dotaApiService;



    // ------------------- VALORANT ENDPOINTS -------------------

    @GetMapping("/sync/{name}/{tag}")
    public String syncPlayer(@PathVariable String name, @PathVariable String tag) {
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

            JsonNode mmrResponse = apiService.getPlayerMMR(region, name, tag);

            if (mmrResponse != null && mmrResponse.has("data")) {
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

        if (player.getLastUpdated() != null &&
                java.time.temporal.ChronoUnit.MINUTES.between(player.getLastUpdated(), java.time.LocalDateTime.now()) < 10) {
            return "Дані актуальні. Завантажено з локальної бази (Кеш).";
        }

        JsonNode response = apiService.getMatchHistory(player.getRegion(), name, tag);

        if (response != null && response.has("data")) {
            matchRepository.deleteByPlayerId(player.getId());
            JsonNode matches = response.path("data");

            double totalKills = 0, totalDeaths = 0, totalHs = 0, totalAdr = 0, totalRating = 0;
            int count = 0;

            for (JsonNode match : matches) {
                String matchId = match.path("metadata").path("matchid").asText();
                String mode = match.path("metadata").path("mode").asText();
                List<String> validModes = List.of("Competitive", "Unrated", "Premier");
                if (!validModes.contains(mode)) {
                    continue; // Якщо це Deathmatch, Custom або Escalation - просто скіпаємо цей матч
                }
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

                        totalKills += kills; totalDeaths += deaths; totalHs += hsPercent;
                        totalAdr += adr; totalRating += finalRating;
                        count++;
                        break;
                    }
                }
            }

            if (count > 0) {
                player.setAverageKd(totalDeaths == 0 ? totalKills : totalKills / totalDeaths);
                player.setAverageHs((int) (totalHs / count));
                player.setAverageAdr((int) (totalAdr / count));
                player.setAverageRating(totalRating / count);
                player.setLastUpdated(java.time.LocalDateTime.now());
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

    // ------------------- GLOBAL ENDPOINTS -------------------

    @GetMapping("/leaderboard")
    public List<Player> getLeaderboard() {
        return playerRepository.findTop10ByOrderByAverageRatingDesc();
    }

    @PostMapping("/link/{name}/{tag}")
    @SecurityRequirement(name = "Bearer Authentication")
    public String linkAccount(@PathVariable String name, @PathVariable String tag, Principal principal) {
        if (principal == null) {
            return "Помилка: Ви не авторизовані!";
        }

        AppUser currentUser = appUserRepository .findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("Користувача не знайдено"));

        Player player = playerRepository.findByRiotIdAndTagLine(name, tag)
                .orElse(new Player());

        player.setRiotId(name);
        player.setTagLine(tag);
        player.setAppUser(currentUser);

        playerRepository.save(player);

        return String.format("Ігровий акаунт %s#%s успішно прив'язано до email: %s", name, tag, currentUser.getEmail());
    }

    // ------------------- CS2 (Faceit) ENDPOINTS -------------------

    @GetMapping("/sync/cs2/{nickname}")
    public String syncCs2FaceitPlayer(@PathVariable String nickname) {
        JsonNode profile = faceitApiService.getPlayerProfile(nickname);
        if (profile == null || !profile.has("player_id")) {
            throw new RuntimeException("Гравця Faceit не знайдено");
        }

        String playerId = profile.path("player_id").asText();
        String avatar = profile.path("avatar").asText();
        JsonNode cs2Data = profile.path("games").path("cs2");

        int faceitLevel = cs2Data != null && cs2Data.has("skill_level") ? cs2Data.path("skill_level").asInt() : 0;
        int faceitElo = cs2Data != null && cs2Data.has("faceit_elo") ? cs2Data.path("faceit_elo").asInt() : 0;

        Player player = playerRepository.findByNicknameIgnoreCaseAndGameType(nickname, GameType.CS2)
                .orElse(new Player());

        player.setSteamId(playerId);
        player.setGameType(GameType.CS2);
        player.setNickname(profile.path("nickname").asText());
        player.setPlayerCard(avatar.isEmpty() ? null : avatar);
        player.setCurrentRank("Level " + faceitLevel + " (" + faceitElo + " ELO)");
        player.setAccountLevel(faceitLevel);

        JsonNode stats = faceitApiService.getPlayerStats(playerId);
        if (stats != null && stats.has("lifetime")) {
            JsonNode lifetime = stats.path("lifetime");
            player.setAverageKd(Double.parseDouble(lifetime.path("Average K/D Ratio").asText("0")));
            player.setAverageHs((int) Double.parseDouble(lifetime.path("Average Headshots %").asText("0")));
            player.setAverageRating(player.getAverageKd());
        }

        player.setLastUpdated(java.time.LocalDateTime.now());
        playerRepository.save(player);
        return "Синхронізовано Faceit: " + nickname;
    }

    @Transactional
    @GetMapping("/matches/cs2/{nickname}")
    public String syncCs2FaceitMatches(@PathVariable String nickname) {
        Player player = playerRepository.findByNicknameIgnoreCaseAndGameType(nickname, GameType.CS2)
                .orElseThrow(() -> new RuntimeException("Спочатку синхронізуйте профіль!"));

        JsonNode history = faceitApiService.getMatchHistory(player.getSteamId());
        if (history != null && history.has("items")) {
            matchRepository.deleteByPlayerId(player.getId());

            for (JsonNode match : history.path("items")) {
                String matchId = match.path("match_id").asText();
                JsonNode matchStats = faceitApiService.getMatchStats(matchId);
                if (matchStats == null || !matchStats.has("rounds")) continue;

                JsonNode roundData = matchStats.path("rounds").get(0);
                String map = roundData.path("round_stats").path("Map").asText();
                String score = roundData.path("round_stats").path("Score").asText();

                for (JsonNode team : roundData.path("teams")) {
                    for (JsonNode p : team.path("players")) {
                        if (p.path("player_id").asText().equals(player.getSteamId())) {
                            JsonNode pStats = p.path("player_stats");
                            MatchData md = new MatchData();
                            md.setMatchId(matchId);
                            md.setMode("Faceit 5v5");
                            md.setMap(map + " (" + score + ")");
                            md.setKills(Integer.parseInt(pStats.path("Kills").asText("0")));
                            md.setDeaths(Integer.parseInt(pStats.path("Deaths").asText("0")));
                            md.setAssists(Integer.parseInt(pStats.path("Assists").asText("0")));
                            md.setHsPercent((int) Double.parseDouble(pStats.path("Headshots %").asText("0")));
                            md.setRating21(Double.parseDouble(pStats.path("K/D Ratio").asText("0")));
                            md.setPlayer(player);
                            md.setAgent("CS2");
                            matchRepository.save(md);
                            break;
                        }
                    }
                }
            }
            return "Матчі Faceit оновлено!";
        }
        return "Не вдалося отримати історію Faceit.";
    }

    @GetMapping("/history/cs2/{nickname}")
    public List<MatchData> getCs2History(@PathVariable String nickname) {
        Player player = playerRepository.findByNicknameIgnoreCaseAndGameType(nickname, GameType.CS2)
                .orElseThrow(() -> new RuntimeException("Гравця не знайдено"));
        return matchRepository.findByPlayerId(player.getId());
    }

    @GetMapping("/profile/cs2/{nickname}")
    public Player getCs2Profile(@PathVariable String nickname) {
        return playerRepository.findByNicknameIgnoreCaseAndGameType(nickname, GameType.CS2)
                .orElseThrow(() -> new RuntimeException("Гравця Faceit не знайдено"));
    }
    @PostMapping("/link/cs2/{nickname}")
    @SecurityRequirement(name = "Bearer Authentication")
    public String linkCs2Account(@PathVariable String nickname, Principal principal) {
        if (principal == null) throw new RuntimeException("Помилка: Ви не авторизовані!");

        AppUser currentUser = appUserRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("Користувача не знайдено"));

        // Шукаємо в БД. Якщо немає — авто-синхронізуємо через API.
        Player player = playerRepository.findByNicknameIgnoreCaseAndGameType(nickname, GameType.CS2)
                .orElseGet(() -> {
                    syncCs2FaceitPlayer(nickname);
                    return playerRepository.findByNicknameIgnoreCaseAndGameType(nickname, GameType.CS2)
                            .orElseThrow(() -> new RuntimeException("API Помилка: не вдалося завантажити профіль Faceit"));
                });

        player.setAppUser(currentUser);
        playerRepository.save(player);

        return String.format("Faceit акаунт %s успішно прив'язано до email: %s", player.getNickname(), currentUser.getEmail());
    }
    // ------------------- DOTA 2 ENDPOINTS -------------------

    @GetMapping("/sync/dota2/{accountId}")
    public String syncDotaPlayer(@PathVariable String accountId) {
        JsonNode profileData = dotaApiService.getPlayerProfile(accountId);
        if (profileData == null || !profileData.has("profile")) {
            throw new RuntimeException("Гравця Dota 2 не знайдено (перевірте ID)");
        }

        JsonNode profileNode = profileData.path("profile");
        String nickname = profileNode.path("personaname").asText();
        String avatar = profileNode.path("avatarfull").asText();

        // ---> ВИПРАВЛЕНО: Використовуємо новий конвертер <---
        String rankTierStr = profileData.path("rank_tier").asText("null");
        String readableRank = convertDotaRank(rankTierStr);

        Player player = playerRepository.findByGameTypeAndSteamId(GameType.DOTA2, accountId)
                .orElse(new Player());

        player.setGameType(GameType.DOTA2);
        player.setSteamId(accountId);
        player.setNickname(nickname);
        player.setPlayerCard(avatar);
        player.setCurrentRank(readableRank); // Тепер сюди запишеться "Herald 3"

        player.setLastUpdated(java.time.LocalDateTime.now());
        playerRepository.save(player);

        return "Синхронізовано Dota 2: " + nickname;
    }

    @Transactional
    @GetMapping("/matches/dota2/{accountId}")
    public String syncDotaMatches(@PathVariable String accountId) {
        Player player = playerRepository.findByGameTypeAndSteamId(GameType.DOTA2, accountId)
                .orElseThrow(() -> new RuntimeException("Спочатку синхронізуйте профіль!"));

        // Тепер отримуємо типізований список
        List<MatchData> matches = dotaApiService.getRecentMatches(accountId);

        if (matches == null || matches.isEmpty()) {
            return "Історія матчів порожня. Переконайтесь, що в налаштуваннях Dota 2 увімкнено 'Expose Public Match Data'.";
        }

        matchRepository.deleteByPlayerId(player.getId());

        double totalRating = 0;
        long totalGpm = 0, totalXpm = 0, totalHeroDmg = 0;
        int count = 0;

        for (MatchData md : matches) {
            md.setPlayer(player); // Прив'язуємо гравця до матчу

            // Отримуємо вже розпарсені метрики
            int kills = md.getKills();
            int deaths = md.getDeaths();
            int assists = md.getAssists();
            int gpm = md.getGpm() != null ? md.getGpm() : 0;
            int xpm = md.getXpm() != null ? md.getXpm() : 0;
            int heroDmg = md.getHeroDamage() != null ? md.getHeroDamage() : 0;

            // Розрахунок імпакту (MOBA Rating)
            double kda = deaths > 0 ? (kills + (assists * 0.5)) / deaths : (kills + (assists * 0.5));
            double farmImpact = (gpm + xpm) / 1000.0;
            double matchRating = (kda * 0.6) + (farmImpact * 0.4);

            md.setRating21(matchRating);
            matchRepository.save(md);

            totalRating += matchRating;
            totalGpm += gpm;
            totalXpm += xpm;
            totalHeroDmg += heroDmg;
            count++;
        }

        // Збереження глобальної MOBA-статистики в профіль гравця
        if (count > 0) {
            player.setAverageRating(totalRating / count);
            player.setAverageGpm((int) (totalGpm / count));
            player.setAverageXpm((int) (totalXpm / count));
            player.setAverageHeroDamage((int) (totalHeroDmg / count));
            player.setLastUpdated(java.time.LocalDateTime.now());
            playerRepository.save(player);
        }
        return "Матчі Dota 2 оновлено!";
    }

    @GetMapping("/profile/dota2/{accountId}")
    public ResponseEntity<Player> getDotaProfile(@PathVariable String accountId) {
        Player player = playerRepository.findByGameTypeAndSteamId(GameType.DOTA2, accountId)
                .orElseThrow(() -> new RuntimeException("Гравця Dota 2 не знайдено"));

        return ResponseEntity.ok(player);
    }

    @GetMapping("/history/dota2/{accountId}")
    public List<MatchData> getDotaHistory(@PathVariable String accountId) {
        Player player = playerRepository.findByGameTypeAndSteamId(GameType.DOTA2, accountId)
                .orElseThrow(() -> new RuntimeException("Гравця не знайдено"));
        return matchRepository.findByPlayerId(player.getId());
    }

    @PostMapping("/link/dota2/{accountId}")
    @SecurityRequirement(name = "Bearer Authentication")
    public String linkDotaAccount(@PathVariable String accountId, Principal principal) {
        if (principal == null) throw new RuntimeException("Помилка: Ви не авторизовані!");

        if (!accountId.matches("\\d+")) {
            throw new IllegalArgumentException("Помилка: Dota 2 ID має складатися лише з цифр.");
        }

        // НОРМАЛІЗАЦІЯ: Завжди працюємо з 32-bit ID, навіть якщо юзер ввів 64-bit
        String normalizedId = dotaApiService.convertToSteamId32(accountId);

        AppUser currentUser = appUserRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("Користувача не знайдено"));

        Player player = playerRepository.findByGameTypeAndSteamId(GameType.DOTA2, normalizedId)
                .orElseGet(() -> {
                    syncDotaPlayer(normalizedId);
                    return playerRepository.findByGameTypeAndSteamId(GameType.DOTA2, normalizedId)
                            .orElseThrow(() -> new RuntimeException("API Помилка: не вдалося завантажити профіль Dota 2."));
                });

        player.setAppUser(currentUser);
        playerRepository.save(player);

        String displayName = player.getNickname() != null ? player.getNickname() : normalizedId;
        return String.format("Dota 2 акаунт %s успішно прив'язано до email: %s", displayName, currentUser.getEmail());
    }

    // Конвертація OpenDota rank_tier у читабельний формат
    private String convertDotaRank(String rankTierStr) {
        if (rankTierStr == null || rankTierStr.equals("null") || rankTierStr.isEmpty()) {
            return "Uncalibrated";
        }

        try {
            int tier = Integer.parseInt(rankTierStr);
            if (tier == 0) return "Uncalibrated";

            int badge = tier / 10;
            int star = tier % 10;

            String[] ranks = {"", "Herald", "Guardian", "Crusader", "Archon", "Legend", "Ancient", "Divine", "Immortal"};

            if (badge >= 1 && badge <= 8) {
                // У Immortal (8) немає зірок у такому ж форматі, тому просто повертаємо назву
                return badge == 8 ? "Immortal" : ranks[badge] + " " + star;
            }
        } catch (NumberFormatException e) {
            return "Uncalibrated";
        }
        return "Unknown";
    }



}