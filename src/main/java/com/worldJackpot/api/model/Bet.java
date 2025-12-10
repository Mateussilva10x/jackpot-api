package com.worldJackpot.api.model;

import jakarta.persistence.*;

@Entity
@Table(name = "bets")
public class Bet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "match_id")
    private Match match;

    private Integer homeScoreGuess;
    private Integer awayScoreGuess;
    private Integer pointsEarned;
}