package com.udtracker.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.udtracker.api.model.GameType;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LinkedAccountDto {
    private Long id;
    private GameType gameType;
    private String steamId;
    private String nickname;
    private String riotId;
    private String tagLine;
    private String currentRank;
    private Double averageRating;
    private String playerCard;
    // ДОДАНО: Специфічні метрики для міжігрової статики
    private Double averageKd;
    private Integer averageGpm;
    private Integer averageXpm;

    // Конструктор для my-profile
    public LinkedAccountDto(Long id, GameType gameType, String nickname, String rank, Double rating, Double kd, Integer gpm, Integer xpm) {
        this.id = id;
        this.gameType = gameType;
        this.nickname = nickname;
        this.currentRank = rank;
        this.averageRating = rating;
        this.averageKd = kd;
        this.averageGpm = gpm;
        this.averageXpm = xpm;
    }
}