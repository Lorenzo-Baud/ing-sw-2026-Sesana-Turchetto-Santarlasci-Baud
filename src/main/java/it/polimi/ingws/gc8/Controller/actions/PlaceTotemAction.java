package it.polimi.ingws.gc8.Controller.actions;

/**
 * Action: a player places their totem on an offer tile.
 * Valid during the POSITIONING phase.
 */
public class PlaceTotemAction implements PlayerAction {

    private static final long serialVersionUID = 1L;

    /** Player id */
    private final int playerId;
    /** Offer tile id */
    private final String offerTileId;

    /**
     * Constructor for the PlaceTotemAction
     * @param playerId player id
     * @param offerTileId offer tile id
     */
    public PlaceTotemAction(int playerId, String offerTileId) {
        this.playerId = playerId;
        this.offerTileId = offerTileId;
    }

    /**
     * Returns player id
     * @return player id
     */
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
