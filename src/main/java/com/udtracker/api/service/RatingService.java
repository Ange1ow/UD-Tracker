package com.udtracker.api.service;

import org.springframework.stereotype.Service;

@Service
public class RatingService {

    /**
     * Розрахунок рейтингу в стилі VLR/HLTV 2.1 для Valorant
     * kpr - вбивства за раунд (найбільший вплив)
     * adr - середня шкода (показує імпакт навіть без кілів)
     * survival - відсоток раундів, де гравець вижив
     */
    public double calculateRating(int kills, int deaths, int adr, int rounds) {
        if (rounds <= 0) return 0.0;

        double kpr = (double) kills / rounds;

        // Захист: якщо смертей більше ніж раундів (наприклад, через воскресіння Sage),
        // виживання не піде в мінус, а просто дорівнюватиме 0.
        double survival = Math.max(0.0, 1.0 - ((double) deaths / rounds));

        // НОВА БАЛАНСНА ФОРМУЛА
        // Якщо зіграти 15/15/5 при 135 ADR за 20 раундів -> Рейтинг буде рівно 1.00
        double rating = (kpr * 0.6) + (adr * 0.003) + (survival * 0.3) + 0.07;

        return Math.round(rating * 100.0) / 100.0;
    }
}