package com.udtracker.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import com.udtracker.api.model.GameType;

@Data
@AllArgsConstructor
public class LinkedAccountDto {
    private GameType gameType;
    private String steamId;
    private String riotId;
    private String tagLine;
    private String currentRank;
    private Double averageRating;
    private String playerCard;
}