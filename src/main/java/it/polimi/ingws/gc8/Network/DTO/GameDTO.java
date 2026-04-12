package it.polimi.ingws.gc8.Network.DTO;

import java.io.Serializable;
import java.util.List;

/**
 * Data Transfer Object that represents a snapshot of the game state
 * to be sent over the network.
 *
 * @param numberOfPlayers       total number of players in the current game
 * @param currentPlayerIndex    index of the player whose turn is currently active
 * @param players               players participating in the game
 * @param upperRowCards         cards currently available in the upper row of the board
 * @param lowerRowCards         cards currently available in the lower row of the board
 * @param offerTrack            DTO representation of the current offer track state
 */
public record GameDTO(
    int numberOfPlayers,
    int currentPlayerIndex,
    List<PlayerDTO> players,
    List<CardDTO> upperRowCards,
    List<CardDTO> lowerRowCards,
    List<OfferTileDTO> offerTrack
) implements Serializable {}
