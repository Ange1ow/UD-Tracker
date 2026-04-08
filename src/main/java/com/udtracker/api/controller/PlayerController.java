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
import com.udtracker.api.model.GameType;
import com.udtracker.api.service.FaceitApiService;

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

        AppUser currentUser = appUserRepository.findByEmail(principal.getName())
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
        if (principal == null) {
            return "Помилка: Ви не авторизовані!";
        }

        AppUser currentUser = appUserRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("Користувача не знайдено"));

        // Шукаємо вже існуючий (завантажений) профіль CS2
        Player player = playerRepository.findByNicknameIgnoreCaseAndGameType(nickname, GameType.CS2)
                .orElseThrow(() -> new RuntimeException("Спочатку знайдіть профіль через пошук!"));

        player.setAppUser(currentUser);
        playerRepository.save(player);

        return String.format("Faceit акаунт %s успішно прив'язано до email: %s", nickname, currentUser.getEmail());
    }
}