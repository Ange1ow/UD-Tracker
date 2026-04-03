package com.udtracker.api.service;

import com.udtracker.api.model.AppUser;
import com.udtracker.api.model.Player;
import com.udtracker.api.repository.AppUserRepository;
import com.udtracker.api.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BackgroundSyncService {

    private static final Logger log = LoggerFactory.getLogger(BackgroundSyncService.class);

    private final PlayerRepository playerRepository;
    private final AppUserRepository appUserRepository;
    private final ExternalApiService apiService;

    // Запускається кожні 2 години (7200000 мс)
    @Scheduled(fixedDelay = 7200000)
    @Transactional
    public void updateActiveProfilesAndGlobalRating() {
        log.info("Запуск фонового оновлення рангів та Global Rating...");

        // 1. Оновлюємо ранги для активних акаунтів
        List<Player> activePlayers = playerRepository.findByAppUserIsNotNull();
        for (Player p : activePlayers) {
            try {
                Thread.sleep(1500); // Thread Sleep для уникнення 429 Too Many Requests від API
                var mmr = apiService.getPlayerMMR(p.getRegion(), p.getRiotId(), p.getTagLine());
                if (mmr != null && mmr.has("data")) {
                    p.setCurrentRank(mmr.path("data").path("current_data").path("currenttierpatched").asText());
                    playerRepository.save(p);
                }
            } catch (Exception e) {
                log.error("Помилка оновлення {}: {}", p.getRiotId(), e.getMessage());
            }
        }

        // 2. Агрегація Global Rating для кожного юзера
        List<AppUser> allUsers = appUserRepository.findAll();
        for (AppUser user : allUsers) {
            if (user.getGameAccounts().isEmpty()) continue;

            double totalRating = 0;
            int validAccounts = 0;

            for (Player acc : user.getGameAccounts()) {
                if (acc.getAverageRating() != null && acc.getAverageRating() > 0) {
                    totalRating += acc.getAverageRating();
                    validAccounts++;
                }
            }

            if (validAccounts > 0) {
                user.setGlobalRating(totalRating / validAccounts);
                appUserRepository.save(user);
            }
        }
        log.info("Фонове оновлення завершено.");
    }
}