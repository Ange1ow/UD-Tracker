package com.udtracker.api.repository; // Перевір, щоб шлях був правильним

import com.udtracker.api.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;
import com.udtracker.api.model.GameType;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {
    Optional<Player> findByRiotIdAndTagLine(String riotId, String tagLine);

    // Змініть другий параметр на GameType
    Optional<Player> findByNicknameIgnoreCaseAndGameType(String nickname, GameType gameType);

    // Додайте цей метод (використовується в контролері)
    Optional<Player> findByGameTypeAndSteamId(GameType gameType, String steamId);

    Optional<Player> findBySteamIdAndGameType(String steamId, GameType gameType);

    List<Player> findTop10ByOrderByAverageRatingDesc();
    List<Player> findByAppUserIsNotNull();
}