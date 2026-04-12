package it.polimi.ingws.gc8.Network.DTO;

import it.polimi.ingws.gc8.Model.Enum.TotemColor;

import java.io.Serializable;
import java.util.List;

/**
 * Data Transfer Object representing the state of a player.
 *
 * @param playerId       unique identifier for the player within the game session
 * @param nickname       chosen display name of the player
 * @param totemColor     color associated with the player's markers
 * @param foodPoints     current amount of food points
 * @param prestigePoints current score or prestige points
 * @param tribe          list of cards in the player's tribe
 */
public record PlayerDTO(
    int playerId,
    String nickname,
    TotemColor totemColor,
    int foodPoints,
    int prestigePoints,
    List<CardDTO> tribe
) implements Serializable {}