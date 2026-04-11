package it.polimi.ingws.gc8.Controller.actions;

/**
 * Action: a player places their totem on an offer tile.
 * Valid during the POSITIONING phase.
 */
public class PlaceTotemAction implements PlayerAction {

    private static final long serialVersionUID = 1L;

    private final int playerId;
    private final String offerTileId;

    public PlaceTotemAction(int playerId, String offerTileId) {
        this.playerId = playerId;
        this.offerTileId = offerTileId;
    }

    @Override
    public int getPlayerId() {
        return playerId;
    }

    /**
     * @return the ID of the chosen offer tile (e.g., "A", "B", ...)
     */
    public String getOfferTileId() {
        return offerTileId;
    }
}
