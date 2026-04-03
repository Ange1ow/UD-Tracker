package com.udtracker.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "matches")
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
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "player_id")
    private Player player;

}