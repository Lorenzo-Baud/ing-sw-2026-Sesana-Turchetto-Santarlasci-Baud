package it.polimi.ingws.gc8.Model;

import it.polimi.ingws.gc8.Model.Cards.BuildingCard;
import it.polimi.ingws.gc8.Model.Cards.CharacterCard;
import it.polimi.ingws.gc8.Model.Cards.CharacterCards.Shaman;
import it.polimi.ingws.gc8.Model.Enum.CharacterType;

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

    // Returns number of collectors
    public int getNumberOfCollectors() {
        int totalCollectors = 0;
        for (CharacterCard card : characters) {
            if (card.getCharacterType() == CharacterType.BUILDER) {
                totalCollectors++;
            }
        }
        return totalCollectors;
    }

    // Returns number of hunters
    public int getNumberOfHunters() {
        int totalHunters = 0;
        for (CharacterCard card : characters) {
            if (card.getCharacterType() == CharacterType.HUNTER) {
                totalHunters++;
            }
        }
        return totalHunters;
    }

    // Returns number of shaman icons
    public int getShamanIcons() {
        int totalIcons = 0;
        for (CharacterCard card : characters) {
            if (card.getCharacterType() == CharacterType.SHAMAN) {
                totalIcons = totalIcons + ((Shaman)card).getnIcons();
            }
        }
        return totalIcons;
    }

    // Returns number of artists
    public int getNumberOfArtists() {
        int totalArtists = 0;
        for (CharacterCard card : characters) {
            if (card.getCharacterType() == CharacterType.ARTIST) {
                totalArtists++;
            }
        }
        return totalArtists;
    }


}
