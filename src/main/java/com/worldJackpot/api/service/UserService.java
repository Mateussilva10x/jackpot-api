package com.worldJackpot.api.service;

import com.worldJackpot.api.dto.bet.BetDto;
import com.worldJackpot.api.dto.user.UserProfileDto;
import com.worldJackpot.api.model.User;
import com.worldJackpot.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final MatchService matchService;

    public UserProfileDto getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new com.worldJackpot.api.exception.ResourceNotFoundException("User not found: " + userId));

        List<BetDto.MatchGroupResponse> userBets = matchService.getMatchesGroupedByGroup(userId, null);

        // Calculate approximate ranking position for display, if desired.
        // For simplicity and reusing existing entity fields, we just use the user fields.
        
        return UserProfileDto.builder()
                .id(user.getId())
                .name(user.getName())
                .totalPoints(user.getTotalPoints() == null ? 0 : user.getTotalPoints())
                .rankingPosition(user.getRankingPosition())
                .avatarId(user.getAvatarId())
                .bets(userBets)
                .build();
    }
}
