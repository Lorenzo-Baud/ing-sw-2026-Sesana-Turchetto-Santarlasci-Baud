package model.Board;

import com.sun.tools.javac.util.List;

import java.util.Arrays;

public class OfferTrack {
    private List<OfferTile> tiles;

    public OfferTrack(List<OfferTile> tiles) {
        this.tiles = List.from(tiles);
    }

    public OfferTile[] getTiles() {
        return tiles.toArray(new OfferTile[0]);
    }

    public void setTiles(OfferTile[] tiles) {
        this.tiles = List.from(tiles);;
    }

    public OfferTile getTileById(OffertileId id) {
        for (OfferTile tile : tiles) {
            if (tile.getId() == id) {
                return tile;
            }
        }
        return null; // or throw an exception if not found
    }

    public boolean isTileFree(OffertileId id) {
        OfferTile tile = getTileById(id);
        return tile != null && tile.getOccupant() == null;
    }

    public void placeTotem(OffertileId id, Player player) {
        OfferTile tile = getTileById(id);
        if (tile != null && tile.getOccupant() == null) {
            tile.setOccupant(player);
        } else {
            throw new IllegalStateException("Tile is either not found or already occupied.");
        }
    }

    public List<OfferTile> getAvailableTiles() {
        return Arrays.stream(new List[]{tiles})
                     .filter(tile -> tile.getOccupant() == null) //a cosa serve getOccupant
                     .toArray(OfferTile[]::new);
    }

    public List<OfferTile> getResolutionOrder() {
        return Arrays.stream(new List[]{tiles})
                     .filter(tile -> tile.getOccupant() != null)
                     .toArray(OfferTile[]::new);
    }

    public void clear() {
        for (OfferTile tile : tiles) {
            tile.setOccupant(null);
        }
    }

    public boolean containsPlayer(Player player) {
        return Arrays.stream(new List[]{tiles})
                     .anyMatch(tile -> tile.getOccupant() != null && tile.getOccupant().equals(player));
    }

    public boolean idFull(){
        return Arrays.stream(new List[]{tiles})
                     .allMatch(tile -> tile.getOccupant() != null);
    }
}
