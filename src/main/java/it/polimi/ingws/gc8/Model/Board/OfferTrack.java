package it.polimi.ingws.gc8.Model.Board;

import java.io.Serializable;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.polimi.ingws.gc8.Model.Player;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Manager for offer tiles available on board
 */
public class OfferTrack implements Serializable {
    private static final long serialVersionUID = 1L;


    /**
     * List of offer tiles on track
     */
    private List<OfferTile> tiles;

    /**
     * Constructs offer track and loads tiles based on player count
     *
     * @param playerCount number of players in game
     */
    public OfferTrack(int playerCount) {
        this.tiles = new ArrayList<>();
        loadTiles(playerCount);
    }

    /**
     * Loads offer tiles from JSON resource file and filters them by player count
     * <p>
     * Uses Jackson ObjectMapper to parse configuration and sorts active tiles by identifier
     * </p>
     *
     * @param playerCount number of players used to filter available tiles
     * @throws RuntimeException if resource file is missing or JSON is malformed
     */
    private void loadTiles(int playerCount) {
        try {
            InputStream is = getClass()
                    .getClassLoader()
                    .getResourceAsStream("offer_tiles.json");

            ObjectMapper mapper = new ObjectMapper();

            List<OfferTile> allTiles = mapper.readValue(is,
                    new TypeReference<List<OfferTile>>() {});

            this.tiles = allTiles.stream()
                    .filter(t -> t.isAvailableFor(playerCount))
                    .sorted(Comparator.comparing(t -> t.getId().name()))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            throw new RuntimeException("Error loading offer_tiles.json", e);
        }
    }

    /**
     * Returns list of offer tiles currently on track
     * @return offer tiles list
     */
    public List<OfferTile> getTiles() { return tiles; }

    /**
     * Finds offer tile by its identifier
     *
     * @param id identifier to search for
     * @return matching tile or null if not found
     */
    public OfferTile getTileById(OffertileId id) {
        for (OfferTile tile : tiles) {
            if (tile.getId() == id) return tile;
        }
        return null;
    }

    /**
     * Checks if specific tile is available for occupation
     *
     * @param id identifier of tile to check
     * @return true if tile exists and has no occupant
     */
    public boolean isTileFree(OffertileId id) {
        OfferTile tile = getTileById(id);
        return tile != null && tile.getOccupant() == null;
    }

    /**
     * Places player totem on specified tile
     *
     * @param id identifier of tile where totem is placed
     * @param player player occupying tile
     * @throws IllegalStateException if tile is missing or already occupied
     */
    public void placeTotem(OffertileId id, Player player) throws IllegalStateException {
        OfferTile tile = getTileById(id);
        if (tile != null && tile.getOccupant() == null) {
            tile.setOccupant(player);
        } else {
            throw new IllegalStateException("Tile not found or already occupied.");
        }
    }

    /**
     * Returns list of tiles currently without occupant
     * @return available tiles list
     */
    public List<OfferTile> getAvailableTiles() {
        return tiles.stream()
                .filter(t -> t.getOccupant() == null)
                .collect(Collectors.toList());
    }

    /**
     * Returns list of occupied tiles in their track order for resolution phase
     * @return occupied tiles list
     */
    public List<OfferTile> getResolutionOrder() {
        return tiles.stream()
                .filter(t -> t.getOccupant() != null)
                .collect(Collectors.toList());
    }

    /**
     * Removes occupants from all tiles on track
     */
    public void clear() {
        tiles.forEach(t -> t.setOccupant(null));
    }

    /**
     * Checks if player already occupies any tile on track
     *
     * @param player player to search for
     * @return true if player is found on a tile
     */
    public boolean containsPlayer(Player player) {
        for (OfferTile t : tiles) {
            if (player.equals(t.getOccupant())) {
                return true;  // trovato, esce subito
            }
        }
        return false;  // non trovato in nessuna tessera
    }

    /**
     * Checks if all players have placed their totem on track
     *
     * @param playerCount total number of players in game
     * @return true if number of occupied tiles equals player count
     */
    public boolean isFull(int playerCount) {
        int occupied = 0;
        for (OfferTile t : tiles) {
            if (t.getOccupant() != null) {
                occupied++;
            }
        }
        return occupied == playerCount;
    }
}