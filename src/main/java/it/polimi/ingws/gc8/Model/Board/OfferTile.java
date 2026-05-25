package it.polimi.ingws.gc8.Model.Board;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.polimi.ingws.gc8.Model.Player;

/**
 * Represents single tile on offer track providing specific rewards and bonuses
 */
public class OfferTile {

    /**
     * Unique identifier for offer tile
     */
    private OffertileId id;

    /**
     * Minimum number of players required for tile to be active in game
     */
    private Integer minPlayers;

    /**
     * Amount of food resources granted by tile
     */
    private int foodReward;

    /**
     * Number of card picks allowed from lower row
     */
    private int lowerRowPicks;

    /**
     * Number of card picks allowed from upper row
     */
    private int upperRowPicks;

    /**
     * Image displayed in the front of the card
     */
    private String image;

    /**
     * Player currently occupying tile
     */
    private Player occupant;

    @JsonCreator
    public OfferTile(
            @JsonProperty("id")            OffertileId id,
            @JsonProperty("minPlayers")    Integer minPlayers,
            @JsonProperty("foodReward")    int foodReward,
            @JsonProperty("lowerRowPicks") int lowerRowPicks,
            @JsonProperty("upperRowPicks") int upperRowPicks,
            @JsonProperty("image")         String image
    ) {
        this.id = id;
        this.minPlayers = minPlayers;
        this.foodReward = foodReward;
        this.lowerRowPicks = lowerRowPicks;
        this.upperRowPicks = upperRowPicks;
        this.occupant = null;  // Initially empty
        this.image = image;
    }


    /**
     * Returns offer tile identifier
     * @return tile id
     */
    public OffertileId getId() {
        return id;
    }

    /**
     * Returns number of card picks from lower row
     * @return lower row picks
     */
    public int getLowerRowPicks() {
        return lowerRowPicks;
    }

    /**
     * Returns minimum players required for tile
     * @return minimum player count
     */
    public Integer getMinPlayers() {
        return minPlayers;
    }

    /**
     * Returns food resource reward
     * @return food reward amount
     */
    public int getFoodReward() {
        return foodReward;
    }

    /**
     * Returns number of card picks from upper row
     * @return upper row picks
     */
    public int getUpperRowPicks() {
        return upperRowPicks;
    }

    /**
     * Returns image path
     * @return image path
     */
    public String getImage() {
        return image;
    }

    /**
     * Returns player currently occupying tile
     * @return tile occupant
     */
    public Player getOccupant() {
        return occupant;
    }

    /**
     * Checks if tile is active based on game player count
     * @param playerCount current number of players
     * @return true if tile is available
     */
    public boolean isAvailableFor(int playerCount) {
        return minPlayers == null || playerCount >= minPlayers;
    }

    /**
     * Sets player occupying tile
     * @param occupant player to set as occupant
     */
    public void setOccupant(Player occupant) {
        this.occupant = occupant;
    }

    /**
     * Checks if tile has no occupant
     * @return true if tile is empty
     */
    public boolean isFree(){
        return occupant == null;
    }

    /**
     * Assigns player totem to tile
     * @param player player placing totem
     */
    public void placeTotem(Player player){
        this.occupant = player;
    }

    /**
     * Removes occupant from tile and returns it
     * @return player that was occupying tile
     */
    public Player removeTotem(){
        Player temp = occupant;
        this.occupant = null;
        return temp;
    }

}
