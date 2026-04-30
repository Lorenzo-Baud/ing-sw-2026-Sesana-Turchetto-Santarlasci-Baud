package it.polimi.ingws.gc8.Network.DTO;

import it.polimi.ingws.gc8.Model.Board.OffertileId;

import java.io.Serializable;

/**
 * Data Transfer Object representing an Offer Tile on the game board
 *
 * @param id                  unique identifier of the offer tile
 * @param playerIndexOccupant playerId of the player currently occupying the tile
 *                            (-1 if empty)
 */
public record OfferTileDTO(
    OffertileId id,
    int playerIndexOccupant
) implements Serializable {}
