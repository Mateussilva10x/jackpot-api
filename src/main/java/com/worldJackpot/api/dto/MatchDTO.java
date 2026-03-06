package com.worldJackpot.api.dto;

import lombok.Data;

@Data
public class MatchDTO {
    private String teamHomeIsoCode;
    private String teamAwayIsoCode;
    private String matchDate;
    private String phase;
    private String groupName;
    private String status;
    private String referenceCode;
}
