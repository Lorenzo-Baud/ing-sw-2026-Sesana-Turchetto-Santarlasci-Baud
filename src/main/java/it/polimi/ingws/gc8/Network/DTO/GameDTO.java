package it.polimi.ingws.gc8.Network.DTO;

import it.polimi.ingws.gc8.Model.Enum.Era;
import it.polimi.ingws.gc8.Model.Enum.GamePhase;
import it.polimi.ingws.gc8.Model.Player;

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
 * @param currentPlayer         DTO that represents the player whose turn is
 * @param phase                 current phase of the game
 * @param currentRound          current round number in the game
 * @param currentEra            current era of the game
 * @param turnOrderTrack        list of players in the order in which they are placed on the turn order track. First
 *                              element is placed on the top slot (will place the totem first),
 *                              last element is placed on the bottom one
 * @param currentUpperTaken     upper-row picks the acting player has already taken this turn (authoritative)
 * @param currentLowerTaken     lower-row picks the acting player has already taken this turn (authoritative)
 * @param currentFoodTaken      whether the acting player has already taken food this turn (authoritative)
 */
public record GameDTO(
    int numberOfPlayers,
    int currentPlayerIndex,
    List<PlayerDTO> players,
    List<CardDTO> upperRowCards,
    List<CardDTO> lowerRowCards,
    List<OfferTileDTO> offerTrack,
    PlayerDTO currentPlayer,
    GamePhase phase,
    int currentRound,
    Era currentEra,
    List<PlayerDTO> turnOrderTrack,
    int currentUpperTaken,
    int currentLowerTaken,
    boolean currentFoodTaken
) implements Serializable {}
