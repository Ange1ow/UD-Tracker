package com.udtracker.api.model;

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

    private Integer gpm;
    private Integer xpm;
    private Integer heroDamage;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "player_id")
    private Player player;


}