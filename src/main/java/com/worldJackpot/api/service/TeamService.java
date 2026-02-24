package com.worldJackpot.api.service;

import com.worldJackpot.api.dto.TeamDTO;
import com.worldJackpot.api.model.Team;
import com.worldJackpot.api.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;

    @Transactional(readOnly = true)
    public List<TeamDTO> getAllTeams() {
        return teamRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private TeamDTO mapToDTO(Team team) {
        TeamDTO dto = new TeamDTO();
        dto.setId(team.getId());
        dto.setName(team.getName());
        dto.setIsoCode(team.getIsoCode());
        dto.setGroup(team.getGroup());
        dto.setFlagUrl(team.getFlagUrl());
        return dto;
    }
}
