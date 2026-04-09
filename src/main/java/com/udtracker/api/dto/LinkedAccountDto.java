package com.udtracker.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import com.udtracker.api.model.GameType;

@Data
@AllArgsConstructor
public class LinkedAccountDto {
    private Object gameType;
    private String steamId;
    private String nickname;
    private String riotId;
    private String tagLine;
    private String currentRank;
    private Double averageRating;
    private String playerCard;

    public LinkedAccountDto(String gameType, String nickname, String rank, Double rating) {
        this.gameType = gameType;
        this.nickname = nickname;
        this.currentRank = rank;
        this.averageRating = rating;
    }
}
