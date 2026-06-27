package it.polimi.ingws.gc8.Model;

import java.io.Serializable;

import it.polimi.ingws.gc8.Model.Cards.BuildingCard.BuildingCard;
import it.polimi.ingws.gc8.Model.Cards.CharacterCards.CharacterCard;
import it.polimi.ingws.gc8.Model.Cards.CharacterCards.Artist;
import it.polimi.ingws.gc8.Model.Cards.CharacterCards.Builder;
import it.polimi.ingws.gc8.Model.Cards.CharacterCards.Collector;
import it.polimi.ingws.gc8.Model.Cards.CharacterCards.Hunter;
import it.polimi.ingws.gc8.Model.Cards.CharacterCards.Inventor;
import it.polimi.ingws.gc8.Model.Cards.CharacterCards.Shaman;
import it.polimi.ingws.gc8.Model.Cards.EventCards.EventCard;
import it.polimi.ingws.gc8.Model.Enum.CharacterType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a player's tribe: the collection of Character cards
 * and Building cards they have acquired during the game.
 * Acts as the central hub for building effect triggers.
 */
public class Tribe implements Serializable {
    private static final long serialVersionUID = 1L;

    /** List of characters inside a tribe */
    private final List<CharacterCard> characters;
    /** List of buildings inside a tribe */
    private final List<BuildingCard> buildings;

    /** Tribe constructor */
    public Tribe() {
        this.characters = new ArrayList<>();
        this.buildings = new ArrayList<>();
    }

    // CARD MANAGEMENT

    /**
     * Adds a Character card to the tribe and triggers
     * onCardAdded on all buildings the player owns.
     * <p>
     * Hunter innate ability: adding a Hunter that carries the food icon
     * immediately grants the player 1 food for every Hunter currently in the
     * tribe (counting all Hunters, with or without the icon, including the one
     * just added).
     *
     * @param player the player who owns this tribe
     * @param card   the Character card being added
     */
    public void addCharacter(Player player, CharacterCard card) {
        characters.add(card);
        triggerOnCardAdded(player, card);

        if (card instanceof Hunter hunter && hunter.hasBonus()) {
            player.addFood(countByType(CharacterType.HUNTER));
        }
    }

    /**
     * Adds a Building card to the tribe.
     *
     * @param card the Building card being added
     */
    public void addBuilding(BuildingCard card) {
        buildings.add(card);
    }

    /**
     * @return the list of Character cards in this tribe
     */
    public List<CharacterCard> getCharacters() {
        return characters;
    }

    /**
     * @return the list of Building cards in this tribe
     */
    public List<BuildingCard> getBuildings() {
        return buildings;
    }

    // BASE COUNTS (from Character cards)

    /**
     * Counts how many Character cards of a given subclass are in the tribe.
     *
     * @param type the CharacterCard subclass to count (e.g. Hunter.class)
     * @return the count
     */
    public int countByType(Class<? extends CharacterCard> type) {
        int count = 0;
        for (CharacterCard card : characters) {
            if (type.isInstance(card)) count++;
        }
        return count;
    }

    /**
     * Counts how many Character cards of a given CharacterType are in the tribe.
     *
     * @param type the CharacterType to count
     * @return the count
     */
    public int countByType(CharacterType type) {
        int count = 0;
        for (CharacterCard card : characters) {
            if (card.getCharacterType() == type) count++;
        }
        return count;
    }

    /**
     * @return total number of Character cards in the tribe
     */
    public int getTotalCharacters() {
        return characters.size();
    }

    /**
     * @return total shaman icons printed on all Shaman cards in the tribe
     */
    public int getTotalShamanIcons() {
        int total = 0;
        for (CharacterCard card : characters) {
            if (card.getCharacterType() == CharacterType.SHAMAN) {
                total += ((Shaman) card).getnIcons();
            }
        }
        return total;
    }

    /**
     * @return total number of Shaman cards in the tribe (not icons, just count)
     */
    public int getTotalShamans() {
        return countByType(Shaman.class);
    }

    /**
     * @return total Builder discount (sum of foodBonus from all Builder cards in the tribe)
     */
    public int getTotalBuilderDiscount() {
        int total = 0;
        for (CharacterCard card : characters) {
            if (card instanceof Builder) {
                total += ((Builder) card).getFoodBonus();
            }
        }
        return total;
    }

    /**
     * @return total food discount from all Collector cards (sum of each Collector's foodDiscount)
     */
    public int getCollectorDiscount() {
        int total = 0;
        for (CharacterCard card : characters) {
            if (card instanceof Collector) {
                total += ((Collector) card).getFoodDiscount();
            }
        }
        return total;
    }

    /**
     * @return number of distinct invention icons across all Inventor cards in the tribe
     */
    public int getDistinctInventionIcons() {
        Set<Integer> icons = new HashSet<>();
        for (CharacterCard card : characters) {
            if (card instanceof Inventor) {
                icons.add(((Inventor) card).getIcon());
            }
        }
        return icons.size();
    }

    /**
     * @return number of Artist pairs in the tribe (artistCount / 2, rounded down)
     */
    public int getArtistPairs() {
        return countByType(Artist.class) / 2;
    }

    /**
     * @return number of distinct Character types present in the tribe (max 6)
     */
    public int getDistinctCharacterTypesCount() {
        Set<CharacterType> types = new HashSet<>();
        for (CharacterCard card : characters) {
            types.add(card.getCharacterType());
        }
        return types.size();
    }

    /**
     * @return sum of prestigeBonus printed on all Building cards in the tribe
     */
    public int getPrintedBuildingPrestige() {
        int total = 0;
        for (BuildingCard card : buildings) {
            total += card.getPrestigeBonus();
        }
        return total;
    }

    /**
     * @return sum of prestigePoints from all Builder cards in the tribe
     */
    public int getBuilderPrestigeSum() {
        int total = 0;
        for (CharacterCard card : characters) {
            if (card instanceof Builder) {
                total += ((Builder) card).getPrestigePoints();
            }
        }
        return total;
    }

    /**
     * Returns the minimum count across all 6 Character types.
     * Used to determine how many complete sets of 6 different types exist.
     *
     * @return minimum count across all 6 types (0 if any type is missing)
     */
    public int getMinCountAcrossAllTypes() {
        int min = Integer.MAX_VALUE;
        for (CharacterType type : CharacterType.values()) {
            int count = 0;
            for (CharacterCard card : characters) {
                if (card.getCharacterType() == type) count++;
            }
            if (count < min) min = count;
        }
        return min;
    }

    // BUILDING EFFECT TRIGGERS

    /**
     * Called when a Character card is added to the tribe.
     * Iterates over all buildings and calls onCardAdded on each effect.
     *
     * @param player the player who owns this tribe
     * @param card   the Character card just added
     */
    private void triggerOnCardAdded(Player player, CharacterCard card) {
        for (BuildingCard building : buildings) {
            building.getEffect().onCardAdded(player, card);
        }
    }

    /**
     * Called during event resolution (Hunt, Cave Painting).
     * Iterates over all buildings and calls onEvent on each effect.
     *
     * @param player the player who owns this tribe
     * @param event  the Event card being resolved
     */
    public void triggerOnEvent(Player player, EventCard event) {
        for (BuildingCard building : buildings) {
            building.getEffect().onEvent(player, event);
        }
    }

    /**
     * Called when the player's Totem returns to the Turn Order tile.
     * Iterates over all buildings and calls onTurnEnd on each effect.
     *
     * @param player    the player who owns this tribe
     * @param slotIndex the slot where the Totem was placed
     */
    public void triggerOnTurnEnd(Player player, int slotIndex) {
        for (BuildingCard building : buildings) {
            building.getEffect().onTurnEnd(player, slotIndex);
        }
    }

    /**
     * Called at the end of the game.
     * Iterates over all buildings and sums the PP returned by onGameEnd.
     *
     * @param player the player who owns this tribe
     * @return total bonus PP from all building effects
     */
    public int triggerOnGameEnd(Player player) {
        int totalBonus = 0;
        for (BuildingCard building : buildings) {
            totalBonus += building.getEffect().onGameEnd(player);
        }
        return totalBonus;
    }

    // BUILDING EFFECT QUERIES

    /**
     * @return total extra shaman icons granted by buildings
     */
    public int getTotalShamanIconBonus() {
        int bonus = 0;
        for (BuildingCard building : buildings) {
            bonus += building.getEffect().getShamanIconBonus();
        }
        return bonus;
    }

    /**
     * @return true if any building prevents PP loss during the Shamanic Ritual
     */
    public boolean hasShamanShield() {
        for (BuildingCard building : buildings) {
            if (building.getEffect().preventsShamanPenalty()) return true;
        }
        return false;
    }

    /**
     * @return true if any building doubles the PP gained during the Shamanic Ritual
     */
    public boolean hasShamanDouble() {
        for (BuildingCard building : buildings) {
            if (building.getEffect().doublesShamanBonus()) return true;
        }
        return false;
    }

    /**
     * @return true if any building grants PP even when tied during the Shamanic Ritual
     */
    public boolean hasShamanGainOnTie() {
        for (BuildingCard building : buildings) {
            if (building.getEffect().gainsOnShamanTie()) return true;
        }
        return false;
    }

    /**
     * @return total food cost reduction during the Sustenance event from all buildings
     */
    public int getTotalSustenanceReduction(Player player) {
        int reduction = 0;
        for (BuildingCard building : buildings) {
            reduction += building.getEffect().getSustenanceFoodReduction(player);
        }
        return reduction;
    }

    /**
     * @return true if any building allows picking one extra card after all Totems return
     */
    public boolean canPickExtraCard() {
        for (BuildingCard building : buildings) {
            if (building.getEffect().canPickExtraCard()) return true;
        }
        return false;
    }

    /**
     * @return total extra food gained when the Totem lands on a Turn Order slot with a food bonus
     */
    public int getTotalExtraFoodOnTurnOrder() {
        int bonus = 0;
        for (BuildingCard building : buildings) {
            bonus += building.getEffect().getExtraFoodOnTurnOrder();
        }
        return bonus;
    }

}
