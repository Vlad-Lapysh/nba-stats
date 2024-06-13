package dev.lapysh.in.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerGameData {
    @JsonIgnore
    private UUID id;
    private String playerName;
    private String teamName;
    private String seasonName;
    private UUID gameId;
    private int points;
    private int rebounds;
    private int assists;
    private int steals;
    private int blocks;
    private int turnovers;
    private int fouls;
    private float minutesPlayed;
}
