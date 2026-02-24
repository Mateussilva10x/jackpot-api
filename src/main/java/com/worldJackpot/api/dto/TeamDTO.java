package com.worldJackpot.api.dto;

import lombok.Data;

@Data
public class TeamDTO {
    private Long id;
    private String name;
    private String isoCode;
    private String group;
    private String flagUrl;
}
