package com.udtracker.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LinkedAccountDto {
    private String riotId;
    private String tagLine;
    private String currentRank;
    private Double averageRating;
    private String playerCard;
}