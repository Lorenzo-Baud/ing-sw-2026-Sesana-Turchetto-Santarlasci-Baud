package it.polimi.ingws.gc8.Model.Board;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.polimi.ingws.gc8.Model.Cards.BuildingCard.BuildingCard;
import it.polimi.ingws.gc8.Model.Cards.Card;
import it.polimi.ingws.gc8.Model.Cards.CharacterCards.CharacterCard;
import it.polimi.ingws.gc8.Model.Cards.CharacterCards.*;
import it.polimi.ingws.gc8.Model.Cards.EventCards.*;
import it.polimi.ingws.gc8.Model.Enum.Era;
import it.polimi.ingws.gc8.Model.Enum.EventType;

import java.io.File;
import java.util.*;

/**
 * Represents the main game board containing all shared components.
 * The Board manages the card decks for different eras, the rows of cards
 * available for purchase, and the tracks for turn order and offers.
 */
public class Board {

    private Deque<Card> tribeDeck;
    private List<Card> lowerRow;
    private List<Card> upperRow;
    private TurnOrderTrack turnOrderTrack;
    private Deque<Card> era2BuildingDeck;
    private Deque<Card> era3BuildingDeck;
    private OfferTrack offerTrack;

    /**
     * Constructs a new Board and initializes all game components based on the number of players.
     * <p>
     * This constructor initializes the card rows as empty lists, creates the decks as
     * {@link ArrayDeque}, and sets up the tracks. Finally, it triggers the loading
     * of the tribe deck.
     * </p>
     *
     * @param playerCount number of players participating in the game,
     * used to calibrate the decks and tracks
     */
    public Board(int playerCount) {
        this.tribeDeck = new ArrayDeque<>();
        this.lowerRow = new ArrayList<>();
        this.upperRow = new ArrayList<>();
        this.era2BuildingDeck = new ArrayDeque<>();
        this.era3BuildingDeck = new ArrayDeque<>();
        this.offerTrack = new OfferTrack(playerCount);
        this.turnOrderTrack = new TurnOrderTrack(playerCount);
        loadTribeDeck(playerCount);
    }

    /**
     * Constructs a Board with pre-initialized components
     * <p>
     * This constructor is primarily intended for testing purposes,
     * allowing the creation of a specific board state
     * </p>
     *
     * @param tribeDeck deck of tribe cards to be used
     * @param lowerRow list of cards to be placed in the lower display row
     * @param upperRow list of cards to be placed in the upper display row
     * @param turnOrderTrack pre-configured turn order
     * @param era2BuildingDeck deck containing Era 2 building cards
     * @param era3BuildingDeck deck containing Era 3 building cards
     * @param offerTrack pre-configured offer track
     */
    public Board(Deque<Card> tribeDeck, List<Card> lowerRow, List<Card> upperRow,
                 TurnOrderTrack turnOrderTrack, Deque<Card> era2BuildingDeck,
                 Deque<Card> era3BuildingDeck, OfferTrack offerTrack) {
        this.tribeDeck = tribeDeck;
        this.lowerRow = lowerRow;
        this.upperRow = upperRow;
        this.turnOrderTrack = turnOrderTrack;
        this.era2BuildingDeck = era2BuildingDeck;
        this.era3BuildingDeck = era3BuildingDeck;
        this.offerTrack = offerTrack;
    }

    /**
     * Loads the tribe deck by parsing character and event cards from JSON files.
     * <p>
     * This method reads cards from `character_cards.json` and `event_cards.json`,
     * filters them based on player count, and organizes them by era. Final event cards
     * are placed at the bottom of the deck. The deck is then shuffled by era before
     * being composed into the final tribe deck structure.
     * </p>
     *
     * @param playerCount the number of players, used to filter cards with player restrictions
     */
    public void loadTribeDeck(int playerCount) {
        ObjectMapper mapper = new ObjectMapper();

        List<Card> eraI = new ArrayList<>();
        List<Card> eraII = new ArrayList<>();
        List<Card> eraIII = new ArrayList<>();
        List<Card> finalEvents = new ArrayList<>();

        // Character card parsing
        try {
            JsonNode root = mapper.readTree(new File("src/main/resources/character_cards.json"));

            for (JsonNode node : root) {
                String type = node.get("type").asText();
                String front = node.get("front").asText();
                String back = node.get("back").asText();
                int eraNum = node.get("era").asInt();
                int players = node.get("number_of_players").asInt();
                Era era = Era.values()[eraNum - 1]; // 1→ERA_1, 2→ERA_2, 3→ERA_3

                // Filters invalid cards for number of players
                if (players != 0 && players > playerCount) continue;

                Card card = null;

                switch (type) {
                    case "artist":
                        card = new Artist(front, back, era, players);
                        break;
                    case "inventor":
                        int icon = node.get("icon").asInt();
                        card = new Inventor(front, back, era, players, icon);
                        break;
                    case "collector":
                        int discount = node.get("foodDiscount").asInt();
                        card = new Collector(front, back, era, players, discount);
                        break;
                    case "shaman":
                        int iconsCount = node.get("icons").asInt();
                        card = new Shaman(front, back, era, iconsCount, players);
                        break;
                    case "hunter":
                        boolean bonus = node.get("bonus").asBoolean();
                        card = new Hunter(front, back, era, players, bonus);
                        break;
                    case "builder":
                        int pPoints = node.get("prestigePoints").asInt();
                        int fBonus = node.get("foodBonus").asInt();
                        card = new Builder(front, back, era, fBonus, pPoints, players);
                        break;
                }

                switch (era) {
                    case ERA_1:
                        eraI.add(card);
                        break;
                    case ERA_2:
                        eraII.add(card);
                        break;
                    case ERA_3:
                        eraIII.add(card);
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // PARSING CARTE EVENTO
        try {
            JsonNode root = mapper.readTree(new File("src/main/resources/event_cards.json"));

            for (JsonNode node : root) {
                String front = node.get("front").asText();
                String back = node.get("back").asText();
                int eraNum = node.get("era").asInt();
                boolean isFinal = node.get("isFinal").asBoolean();
                Era era = Era.values()[eraNum - 1]; // 1→ERA_1, 2→ERA_2, 3→ERA_3
                String type = node.get("type").asText();

                Card card = null;

                switch (type) {
                    case "sustenance":
                        int prestigeMalus = node.get("prestigeMalus").asInt();
                        card = new SustenanceEventCard(front, back, era, isFinal, EventType.sustenance, prestigeMalus);
                        break;
                    case "hunt":
                        int prestigeBonus = node.get("prestigeBonus").asInt();
                        card = new HuntEventCard(front, back, era, isFinal, EventType.hunt, prestigeBonus);
                        break;
                    case "cavePaintings":
                        int bonusArtists = node.get("bonusArtists").asInt();
                        int malusArtists = node.get("malusArtists").asInt();
                        int bonusPoints = node.get("bonusPoints").asInt();
                        int malusPoints = node.get("malusPoints").asInt();
                        card = new CavePaintingsEventCard(
                                front, back, era, isFinal, EventType.cavePaintings,
                                bonusArtists, malusArtists, bonusPoints, malusPoints
                        );
                        break;
                    case "ritual":
                        int ritualBonus = node.get("prestigeBonus").asInt();
                        int ritualMalus = node.get("prestigeMalus").asInt();
                        card = new RitualEventCard(front, back, era, isFinal, EventType.ritual, ritualBonus, ritualMalus);
                        break;
                }

                // Final events cards -> separate list, always on the
                // bottom of the deck
                if (isFinal) {
                    finalEvents.add(card);
                } else {
                    switch (era) {
                        case ERA_1:
                            eraI.add(card);
                            break;
                        case ERA_2:
                            eraII.add(card);
                            break;
                        case ERA_3:
                            eraIII.add(card);
                            break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // COMPOSIZIONE MAZZO FINALE

        // mescola separatamente ogni Era
        Collections.shuffle(eraI);
        Collections.shuffle(eraII);
        Collections.shuffle(eraIII);

        // compone il mazzo: ERA_1 in cima, poi ERA_2, ERA_3, Evento Finale in fondo
        tribeDeck.clear();
        for (Card c : eraI)        tribeDeck.addLast(c);
        for (Card c : eraII)       tribeDeck.addLast(c);
        for (Card c : eraIII)      tribeDeck.addLast(c);
        for (Card c : finalEvents) tribeDeck.addLast(c);
    }

    /**
     * Returns tribe deck
     * @return tribe deck
     */
    public Deque<Card> getTribeDeck() { return tribeDeck; }
    /**
     * Sets tribe cards deck
     * @param tribeDeck tribe deck to set
     */
    public void setTribeDeck(Deque<Card> tribeDeck) { this.tribeDeck = tribeDeck; }

    /**
     * Returns lower row cards
     * @return lower row list
     */
    public List<Card> getLowerRow() { return lowerRow; }
    /**
     * Sets lower row cards
     * @param lowerRow lower row list to set
     */
    public void setLowerRow(List<Card> lowerRow) { this.lowerRow = lowerRow; }

    /**
     * Returns upper row cards
     * @return upper row list
     */
    public List<Card> getUpperRow() { return upperRow; }
    /**
     * Sets upper row cards
     * @param upperRow upper row list to set
     */
    public void setUpperRow(List<Card> upperRow) { this.upperRow = upperRow; }

    /**
     * Returns turn order track manager
     * @return turn order track
     */
    public TurnOrderTrack getTurnOrderTrack() { return turnOrderTrack; }
    /**
     * Sets turn order track manager
     * @param turnOrderTrack turn order track to set
     */
    public void setTurnOrderTrack(TurnOrderTrack turnOrderTrack) { this.turnOrderTrack = turnOrderTrack; }

    /**
     * Returns era 2 building deck
     * @return era 2 deck
     */
    public Deque<Card> getEra2BuildingDeck() { return era2BuildingDeck; }
    /**
     * Sets era 2 building deck
     * @param era2BuildingDeck era 2 deck to set
     */
    public void setEra2BuildingDeck(Deque<Card> era2BuildingDeck) { this.era2BuildingDeck = era2BuildingDeck; }

    /**
     * Returns era 3 building deck
     * @return era 3 deck
     */
    public Deque<Card> getEra3BuildingDeck() { return era3BuildingDeck; }
    /**
     * Sets era 3 building deck
     * @param era3BuildingDeck era 3 deck to set
     */
    public void setEra3BuildingDeck(Deque<Card> era3BuildingDeck) { this.era3BuildingDeck = era3BuildingDeck; }

    /**
     * Returns offer track manager
     * @return offer track
     */
    public OfferTrack getOfferTrack() { return offerTrack; }
    /**
     * Sets offer track manager
     * @param offerTrack offer track to set
     */
    public void setOfferTrack(OfferTrack offerTrack) { this.offerTrack = offerTrack; }

    /**
     * Sets up initial cards in lower and upper rows based on player count
     * @param playerCount number of players in game
     * @param era1Building list of era 1 building cards to distribute
     */
    public void setupInitialRows(int playerCount, List<BuildingCard> era1Building) {
        for (int i = 0; i < playerCount + 1; i++) {
            lowerRow.add(era1Building.get(i));
        }
        for (int i = playerCount + 1; i < playerCount * 2 + 2; i++) {
            upperRow.add(era1Building.get(i));
        }
    }


    /**
     * Returns card from lower row at specified index
     *
     * @param index position of card in list
     * @return card at given index
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public Card getLowerRowCard(int index) {
        if (index >= 0 && index < lowerRow.size()) {
            return lowerRow.get(index);
        }
        throw new IndexOutOfBoundsException("Invalid index for lower row: " + index);
    }

    /**
     * Returns card from upper row at specified index
     *
     * @param index position of card in list
     * @return card at given index
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public Card getUpperRowCard(int index) {
        if (index >= 0 && index < upperRow.size()) {
            return upperRow.get(index);
        }
        throw new IndexOutOfBoundsException("Invalid index for upper row: " + index);
    }

    /**
     * Removes and returns card from lower row at specified index
     *
     * @param index position of card in list
     * @return card that was removed
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public Card removeLowerRowCard(int index) {
        if (index >= 0 && index < lowerRow.size()) {
            return lowerRow.remove(index);
        }
        throw new IndexOutOfBoundsException("Invalid index for lower row: " + index);
    }


    /**
     * Removes and returns card from upper row at specified index
     *
     * @param index position of card to remove
     * @return removed card
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public Card removeUpperRowCard(int index) {
        if (index >= 0 && index < upperRow.size()) {
            return upperRow.remove(index);
        }
        throw new IndexOutOfBoundsException("Invalid index for upper row: " + index);
    }


    /**
     * Filters and returns all event cards from lower row
     *
     * @return list of event cards found in lower row
     */
    public List<EventCard> getLowerRowEvents() {
        List<EventCard> events = new ArrayList<>();
        for (Card card : lowerRow) {
            if (card instanceof EventCard) {
                events.add((EventCard) card);
            }
        }
        return events;
    }

    /**
     * Removes all tribe cards from lower row
     */
    public void discardLowerRowTribeCards() {
        lowerRow.removeIf(card -> card instanceof CharacterCard || card instanceof EventCard);
    }

    /**
     * Moves all upper row cards to the lower row
     */
    public void moveUpperRowTribeCardsToLower() {
        List<Card> tribeCards = new ArrayList<>();
        for (Card card : upperRow) {
            if (card instanceof CharacterCard || card instanceof EventCard) {
                tribeCards.add(card);
            }
        }
        upperRow.removeAll(tribeCards);
        lowerRow.addAll(tribeCards);
    }

    /**
     * Refills the upper row with cards
     *
     * @param playerCount number of players
     * @return list of new tribe cars added to upper row
     */
    public List<Card> refillUpperRow(int playerCount) {
        List<Card> newTribeCards = new ArrayList<>();
        for (int i = 0; i < playerCount + 1; i++) {
            if (!tribeDeck.isEmpty()) {
                Card card = tribeDeck.removeFirst();
                upperRow.add(card);
                newTribeCards.add(card);
            }
        }
        return newTribeCards;
    }

    /**
     * Removes all building cards from lower row
     */
    public void discardLowerRowBuilding() {
        lowerRow.removeIf(card -> card instanceof BuildingCard);
    }

    /**
     * Moves upper row buildings to lower row
     */
    public void moveUpperRowBuildingToLower() {
        List<Card> buildingCards = new ArrayList<>();
        for (Card card : upperRow) {
            if (card instanceof BuildingCard) {
                buildingCards.add(card);
            }
        }
        upperRow.removeAll(buildingCards);
        lowerRow.addAll(buildingCards);
    }

    /**
     * Reveals all building cards from specified era
     *
     * @param era era number to reveal buildings from
     */
    public void revealEraBuildings(int era) {
        Deque<Card> buildingDeck = (era == 2) ? era2BuildingDeck : era3BuildingDeck;
        for (int i = 0; i < 4; i++) {
            if (!buildingDeck.isEmpty()) {
                upperRow.add(buildingDeck.removeLast());
            }
        }
    }
}