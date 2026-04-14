package it.polimi.ingws.gc8.Network.Messages;

public class FoodTakenMessage implements NetworkMessage{

    private final int playerFood;

    public FoodTakenMessage( int playerFood) {
        this.playerFood = playerFood;
    }

    public int getPlayerFood() {
        return playerFood;
    }
}
