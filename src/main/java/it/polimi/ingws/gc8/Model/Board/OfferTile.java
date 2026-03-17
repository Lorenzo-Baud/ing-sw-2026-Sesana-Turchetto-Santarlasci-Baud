package it.polimi.ingws.gc8.Model.Board;

import it.polimi.ingws.gc8.Model.Player;

public class OfferTile {
    private OffertileId id;
    private int minPlayers;
    private int foodReward;
    private int lowerRowPicks;
    private int upperRowPicks;
    private Player occupant;

    public OfferTile(OffertileId id, int minPlayers, int foodReward, int lowerRowPicks, int upperRowPicks) {
        this.id = id;
        this.minPlayers = minPlayers;
        this.foodReward = foodReward;
        this.lowerRowPicks = lowerRowPicks;
        this.upperRowPicks = upperRowPicks;
        this.occupant = occupant;
    }

    public OffertileId getId() {
        return id;
    }

    public int getLowerRowPicks() {
        return lowerRowPicks;
    }

    public void setLowerRowPicks(int lowerRowPicks) {
        this.lowerRowPicks = lowerRowPicks;
    }

    public void setId(OffertileId id) {
        this.id = id;
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public void setMinPlayers(int minPlayers) {
        this.minPlayers = minPlayers;
    }

    public int getFoodReward() {
        return foodReward;
    }

    public void setFoodReward(int foodReward) {
        this.foodReward = foodReward;
    }

    public int getUpperRowPicks() {
        return upperRowPicks;
    }

    public void setUpperRowPicks(int upperRowPicks) {
        this.upperRowPicks = upperRowPicks;
    }

    public Player getOccupant() {
        return occupant;
    }

    public void setOccupant(Player occupant) {
        this.occupant = occupant;
    }

    public boolean isAvailable(int playerCount){

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
