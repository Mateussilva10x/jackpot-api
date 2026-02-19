package com.worldJackpot.api.util;

import com.worldJackpot.api.model.Match;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class MatchDeadlineValidator {

    public void validate(Match match) {
        if (LocalDateTime.now().isAfter(match.getMatchDate())) {
            throw new IllegalArgumentException("Cannot place bet. Match " + match.getId() + " has already started or finished.");
        }
    }
}
