package it.polimi.ingws.gc8.Model.Board;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.polimi.ingws.gc8.Model.Player;

public class OfferTile {
    private OffertileId id;
    private Integer minPlayers;
    private int foodReward;
    private int lowerRowPicks;
    private int upperRowPicks;
    private Player occupant;

    @JsonCreator
    public OfferTile(
            @JsonProperty("id")            OffertileId id,
            @JsonProperty("minPlayers")    Integer minPlayers,
            @JsonProperty("foodReward")    int foodReward,
            @JsonProperty("lowerRowPicks") int lowerRowPicks,
            @JsonProperty("upperRowPicks") int upperRowPicks
    ) {
        this.id = id;
        this.minPlayers = minPlayers;
        this.foodReward = foodReward;
        this.lowerRowPicks = lowerRowPicks;
        this.upperRowPicks = upperRowPicks;
        this.occupant = null;  // inizialmente vuota
    }

    public OffertileId getId() {
        return id;
    }

    public int getLowerRowPicks() {
        return lowerRowPicks;
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public int getFoodReward() {
        return foodReward;
    }

    public int getUpperRowPicks() {
        return upperRowPicks;
    }

    public Player getOccupant() {
        return occupant;
    }

    public boolean isAvailableFor(int playerCount) {
        return minPlayers == null || playerCount >= minPlayers;
    }

    public void setOccupant(Player occupant) {
        this.occupant = occupant;
    }

    public boolean isFree(){
        return occupant == null;
    }

    public void placeTotem(Player player){
        this.occupant = player;
    }

    public Player removeTotem(){
        Player temp = occupant;
        this.occupant = null;
        return temp;
    }
}
