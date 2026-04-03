package com.udtracker.api.repository;

import com.udtracker.api.model.MatchData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface MatchRepository extends JpaRepository<MatchData, Long> {

    // Цей метод виправляє помилку "Cannot resolve method 'findByPlayerId'"
    List<MatchData> findByPlayerId(Long playerId);

    @Transactional
    @Modifying
    @Query("DELETE FROM MatchData m WHERE m.player.id = :playerId")
    void deleteByPlayerId(@Param("playerId") Long playerId);

    @Query("SELECT AVG(m.rating21) FROM MatchData m WHERE m.player.id = :playerId")
    Double getAverageRating(@Param("playerId") Long playerId);
}