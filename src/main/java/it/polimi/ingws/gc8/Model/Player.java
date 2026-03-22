package it.polimi.ingws.gc8.Model;

import it.polimi.ingws.gc8.Model.Cards.BuildingCard;
import it.polimi.ingws.gc8.Model.Cards.CharacterCard;
import it.polimi.ingws.gc8.Model.Enum.TotemColor;

public class Player {

    // Attributes
    private final int playerId;
    private final String nickname;
    private final TotemColor totemColor;
    private int foodPoints;
    private int prestigePoints;
    private final Tribe tribe;

    // Constructor
    public Player(int playerId, String nickname, TotemColor totemColor) {
        this.playerId = playerId;
        this.nickname = nickname;
        this.totemColor = totemColor;
        this.foodPoints = 0;
        this.prestigePoints = 0;
        this.tribe = new Tribe();
    }

    // Food management
    public int getFood() {
        return foodPoints;
    }

    public void addFood(int amount) {
        // TODO: implement
    }

    public void removeFood(int amount) {
        // TODO: implement
    }

    // Prestige management
    public int getPrestigePoints() {
        return prestigePoints;
    }

    public void addPrestigePoints(int amount) {
        // TODO: implement
    }

    public void removePrestigePoints(int amount) {
        // TODO: implement
    }

    // Tribe delegation
    public Tribe getTribe() {
        return tribe;
    }

    public void addCharacterCard(CharacterCard card) {
        tribe.addCharacter(card);
    }

    public void addBuildingCard(BuildingCard card) {
        tribe.addBuilding(card);
    }

    // Tribe aggregated queries (delegated to Tribe)
    public int getTotalBuildingDiscount() {
        // TODO: implement (based on Collector cards in tribe)
        return 0;
    }

    public int getTotalCharacters() {
        return tribe.getCharacters().size();
    }

    public int getTotalShamans() {
        return tribe.getTotalShamans();
    }

    // Getters for identity fields
    public int getPlayerId() {
        return playerId;
    }

    public String getNickname() {
        return nickname;
    }

    public TotemColor getTotemColor() {
        return totemColor;
    }

}
