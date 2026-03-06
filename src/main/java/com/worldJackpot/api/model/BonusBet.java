package com.worldJackpot.api.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "bonus_bets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BonusBet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "champion_team_id")
    private Team championTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "runner_up_team_id")
    private Team runnerUpTeam;

    private String topScorer;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
