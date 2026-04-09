package com.udtracker.api.service;

import com.udtracker.api.model.GameType;
import com.udtracker.api.model.Player;

public interface GameStatsProvider {
    GameType getGameType();
    void updatePlayerStats(Player player);
}