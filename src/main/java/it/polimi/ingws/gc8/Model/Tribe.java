package it.polimi.ingws.gc8.Model;

import it.polimi.ingws.gc8.Model.Cards.BuildingCard;
import it.polimi.ingws.gc8.Model.Cards.CharacterCard;

import java.util.ArrayList;
import java.util.List;

public class Tribe {

    private final List<CharacterCard> characters;
    private final List<BuildingCard> buildings;

    public Tribe() {
        this.characters = new ArrayList<>();
        this.buildings = new ArrayList<>();
    }

    // Add cards
    public void addCharacter(CharacterCard card) {
        characters.add(card);
    }

    public void addBuilding(BuildingCard card) {
        buildings.add(card);
    }

    // Getters
    public List<CharacterCard> getCharacters() {
        return characters;
    }

    public List<BuildingCard> getBuildings() {
        return buildings;
    }

    // Count characters by type
    public int countByType(Class<? extends CharacterCard> type) {
        // TODO: implement
        return 0;
    }

    // Total number of Shaman cards
    public int getTotalShamans() {
        // TODO: implement
        return 0;
    }

    // Sum of food discount from all Collector cards
    public int getCollectedDiscount() {
        // TODO: implement
        return 0;
    }

    // Number of distinct Inventor icons
    public int getDistinctInventorIcons() {
        // TODO: implement
        return 0;
    }

    // Prestige points from Artist cards
    public int getArtistPoints() {
        // TODO: implement
        return 0;
    }

    // Number of distinct character types present in the tribe
    public int getDistinctCharacterTypesCount() {
        // TODO: implement
        return 0;
    }

    // Sum of printed prestige bonuses on building cards
    public int getPrintedBuildingPrestige() {
        // TODO: implement
        return 0;
    }

}
