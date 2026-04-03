package com.udtracker.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "players")
@Data
public class Player {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String riotId;
    private String tagLine;
    private Integer accountLevel;
    private String region;
    private String currentRank;
    private String playerCard;
    private String title;


    // Додамо статистику останнього оновлення
    private LocalDateTime lastUpdated;
    private Double averageRating; // Додай це поле

    private Double averageKd;
    private Integer averageHs;
    private Integer averageAdr;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "app_user_id")
    private AppUser appUser;

    @JsonProperty("isLinked")
    public boolean isLinked() {
        return this.appUser != null;
    }
}