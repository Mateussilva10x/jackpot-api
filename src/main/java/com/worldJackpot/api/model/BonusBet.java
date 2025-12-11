package com.worldJackpot.api.model;

import jakarta.persistence.*;

@Entity
@Table(name = "bonus_bets")
public class BonusBet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "champion_team_id")
    private Team championTeam;

    @ManyToOne
    @JoinColumn(name = "runner_up_team_id")
    private Team runnerUpTeam;

    private String topScorerName;
}
