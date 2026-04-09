package com.udtracker.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "players", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"game_type", "riot_id", "tag_line"}),
        @UniqueConstraint(columnNames = {"game_type", "steam_id"})
})
@Getter
@Setter
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "appUser"})
public class Player {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "game_type", nullable = false)
    private GameType gameType;

    @Column(name = "steam_id")
    private String steamId;

    private String nickname;

    @Column(name = "riot_id")
    private String riotId;

    @Column(name = "tag_line")
    private String tagLine;

    private Integer accountLevel;
    private String region;
    private String currentRank;
    private String playerCard;
    private String title;

    private LocalDateTime lastUpdated;
    private Double averageRating;
    private Double averageKd;
    private Integer averageHs;
    private Integer averageAdr;

    // НОВІ ПОЛЯ ДЛЯ MOBA (Dota 2)
    private Integer averageGpm;
    private Integer averageXpm;
    private Integer averageHeroDamage;
    private Integer averageTowerDamage;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "app_user_id")
    private AppUser appUser;

    @JsonProperty("isLinked")
    public boolean isLinked() {
        return this.appUser != null;
    }
    @JsonIgnore
    public AppUser getAppUser() {
        return this.appUser;
    }
}