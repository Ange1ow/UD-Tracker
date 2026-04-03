package com.udtracker.api.repository; // Перевір, щоб шлях був правильним

import com.udtracker.api.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository // Це ОБОВ'ЯЗКОВО, щоб Spring побачив цей клас
public interface PlayerRepository extends JpaRepository<Player, Long> {
    Optional<Player> findByRiotIdAndTagLine(String riotId, String tagLine);
    List<Player> findTop10ByOrderByAverageRatingDesc();
    List<Player> findByAppUserIsNotNull();
}