package com.worldJackpot.api.dto;

import lombok.Data;
import java.util.List;

@Data
public class WorldCupDataDTO {
    private List<TeamDTO> teams;
    private List<MatchDTO> matches;
}
