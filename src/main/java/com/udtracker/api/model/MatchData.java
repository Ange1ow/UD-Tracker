package com.udtracker.api.model;

import com.fasterxml.jackson.annotation.JsonAlias; // ДОДАНО
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "matches")
@Getter @Setter
@Data
public class MatchData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String matchId;
    private String map;
    private int kills;
    private int deaths;
    private int assists;
    private int adr;
    private double rating21;
    private String agent;
    private Integer hsPercent;
    private String mode;

    // Вказуємо мапінг для OpenDota
    @JsonAlias("gold_per_min")
    private Integer gpm;

    @JsonAlias("xp_per_min")
    private Integer xpm;

    @JsonAlias("hero_damage")
    private Integer heroDamage;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "player_id")
    private Player player;
}