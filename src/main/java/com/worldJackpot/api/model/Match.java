package com.worldJackpot.api.model;

import com.worldJackpot.api.model.enums.MatchPhase;
import com.worldJackpot.api.model.enums.MatchStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "matches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Match {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "team_home_id")
    private Team teamHome;

    @ManyToOne
    @JoinColumn(name = "team_away_id")
    private Team teamAway;

    private LocalDateTime matchDate;

    @Enumerated(EnumType.STRING)
    private MatchPhase phase;

    private String groupName;
    private Integer homeScore;
    private Integer awayScore;

    @Enumerated(EnumType.STRING)
    private MatchStatus status;

    private String referenceCode;

    private Long nextMatchId;

    @Enumerated(EnumType.STRING)
    private com.worldJackpot.api.model.enums.NextMatchSlot nextMatchSlot;
}
