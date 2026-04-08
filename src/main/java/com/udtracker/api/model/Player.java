package com.udtracker.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "players", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"gameType", "riotId", "tagLine"}),
        @UniqueConstraint(columnNames = {"gameType", "steamId"})
})
@Data
public class Player {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Змініть тип поля та додайте анотацію @Enumerated
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameType gameType; // було String
    private String steamId;
    private String nickname;

    private String riotId;
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

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "app_user_id")
    private AppUser appUser;

    @JsonProperty("isLinked")
    public boolean isLinked() {
        return this.appUser != null;
    }
}