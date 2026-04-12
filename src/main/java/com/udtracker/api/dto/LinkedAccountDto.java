package com.udtracker.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.udtracker.api.model.GameType;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LinkedAccountDto {
    private Long id; // Додано для універсальної ідентифікації акаунта на фронтенді
    private GameType gameType;
    private String steamId;
    private String nickname;
    private String riotId;
    private String tagLine;
    private String currentRank;
    private Double averageRating;
    private String playerCard;

    // Конструктор для скороченого відображення (використовується в my-profile)
    public LinkedAccountDto(Long id, GameType gameType, String nickname, String rank, Double rating) {
        this.id = id;
        this.gameType = gameType;
        this.nickname = nickname;
        this.currentRank = rank;
        this.averageRating = rating;
    }
}