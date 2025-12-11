package com.worldJackpot.api.model;

import jakarta.persistence.*;

@Entity
@Table(name = "teams")
public class Team {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String flagUrl;

    @Column(name = "team_group")
    private String group;

    private String isoCode;
}