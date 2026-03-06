package com.worldJackpot.api.service;

import com.worldJackpot.api.dto.user.UserProfileDto;
import com.worldJackpot.api.model.User;
import com.worldJackpot.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MatchService matchService;

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setup() {
        user = new User();
        user.setId(1L);
        user.setName("Test User");
        user.setTotalPoints(100);
        user.setRankingPosition(5);
    }

    @Test
    void testGetUserProfile_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(matchService.getMatchesGroupedByGroup(1L, null)).thenReturn(new ArrayList<>());

        UserProfileDto dto = userService.getUserProfile(1L);

        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("Test User", dto.getName());
        assertEquals(100, dto.getTotalPoints());
        assertEquals(5, dto.getRankingPosition());
        assertEquals(0, dto.getBets().size());
    }
}
