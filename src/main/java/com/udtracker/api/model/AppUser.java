package com.udtracker.api.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "app_users")
@Data
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    // Глобальний міжігровий рейтинг
    private Double globalRating;

    // Зв'язок: Один користувач може мати багато ігрових акаунтів (смурфи, різні ігри)
    @OneToMany(mappedBy = "appUser", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Player> gameAccounts = new ArrayList<>();
}