package it.polimi.ingws.gc8.Model.Board;

import java.io.Serializable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.polimi.ingws.gc8.Model.Cards.BuildingCard.BuildingCard;
import it.polimi.ingws.gc8.Model.Cards.Card;
import it.polimi.ingws.gc8.Model.Cards.CharacterCards.CharacterCard;
import it.polimi.ingws.gc8.Model.Cards.CharacterCards.*;
import it.polimi.ingws.gc8.Model.Cards.EventCards.*;
import it.polimi.ingws.gc8.Model.Enum.CardType;
import it.polimi.ingws.gc8.Model.Enum.Era;
import it.polimi.ingws.gc8.Model.Enum.EventType;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents the main game board containing all shared components.
 * The Board manages the card decks for different eras, the rows of cards
 * available for purchase, and the tracks for turn order and offers.
 */
public class Board implements Serializable {
    private static final long serialVersionUID = 1L;


    private static final Logger LOGGER = Logger.getLogger(Board.class.getName());

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
        List<BuildingCard> era1Buildings = loadBuildingDecks(playerCount);
        dealInitialRows(playerCount, era1Buildings);
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

            InputStream is = Board.class.getResourceAsStream("/character_cards.json");

            if (is == null) {
                throw new FileNotFoundException("Impossibile trovare character_cards.json nelle risorse del JAR");
            }

            JsonNode root = mapper.readTree(is);

            // Characters cards parsing
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
            LOGGER.log(Level.SEVERE, "Failed to parse character_cards.json: tribe deck may be incomplete", e);
        }

        // Event cards parsing
        try {

            InputStream is = Board.class.getResourceAsStream("/event_cards.json");

            if (is == null) {
                throw new FileNotFoundException("Impossibile trovare event_cards.json nelle risorse del JAR");
            }

            JsonNode root = mapper.readTree(is);

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
            LOGGER.log(Level.SEVERE, "Failed to parse event_cards.json: tribe deck may be incomplete", e);
        }

        // Final deck composition

        // Shuffles decks separately by era
        Collections.shuffle(eraI);
        Collections.shuffle(eraII);
        Collections.shuffle(eraIII);

        // Final deck composition: Era I cards on top, then Era II, then
        // Era III, and finally the final events on the bottom
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
     * Loads Building cards from JSON, splits them by Era and, randomly selects for each Era a subset of cards
     * whose size depends on {@code playerCount}:
     * <ul>
     *   <li>2 players: 1 / 2 / 3 cards (Era I / II / III)</li>
     *   <li>3 players: 2 / 2 / 4</li>
     *   <li>4 players: 2 / 3 / 4</li>
     *   <li>5 players: 2 / 3 / 5</li>
     * </ul>
     * The Era II and Era III subsets form the covered building decks on the
     * board; the Era I subset is returned so that the caller can place those
     * cards face up in the upper row next to the initial Tribe cards.
     *
     * @param playerCount number of players, used to size each Era subset
     * @return the Era I Building cards selected for this game
     */
    private List<BuildingCard> loadBuildingDecks(int playerCount) {
        List<BuildingCard> all = BuildingCard.loadFromJson(
                "/buildings_cards.json");

        List<BuildingCard> era1 = new ArrayList<>();
        List<BuildingCard> era2 = new ArrayList<>();
        List<BuildingCard> era3 = new ArrayList<>();
        for (BuildingCard b : all) {
            switch (b.getEra()) {
                case ERA_1: era1.add(b); break;
                case ERA_2: era2.add(b); break;
                case ERA_3: era3.add(b); break;
            }
        }

        int[] buildingCounts = buildingCountsFor(playerCount);
        List<BuildingCard> selectedEra1 = selectRandomBuildings(era1, buildingCounts[0]);
        List<BuildingCard> selectedEra2 = selectRandomBuildings(era2, buildingCounts[1]);
        List<BuildingCard> selectedEra3 = selectRandomBuildings(era3, buildingCounts[2]);

        era2BuildingDeck.clear();
        for (BuildingCard b : selectedEra2) era2BuildingDeck.addLast(b);
        era3BuildingDeck.clear();
        for (BuildingCard b : selectedEra3) era3BuildingDeck.addLast(b);

        return selectedEra1;
    }

    private int[] buildingCountsFor(int playerCount) {
        switch (playerCount) {
            case 2:
                return new int[] {1, 2, 3};
            case 3:
                return new int[] {2, 2, 4};
            case 4:
                return new int[] {2, 3, 4};
            case 5:
                return new int[] {2, 3, 5};
            default:
                throw new IllegalArgumentException("Unsupported player count: " + playerCount);
        }
    }

    private List<BuildingCard> selectRandomBuildings(List<BuildingCard> cards, int count) {
        if (cards.size() < count) {
            throw new IllegalStateException("Not enough Building cards for setup");
        }
        Collections.shuffle(cards);
        return new ArrayList<>(cards.subList(0, count));
    }

    /**
     * Fills the lower and upper rows at game start, per the preparation rules:
     * <ul>
     *   <li>Rule 4: draw from the tribe deck until the lower row holds
     *       {@code playerCount + 1} non-Event Tribe cards; any Event drawn is
     *       redirected to the upper row.</li>
     *   <li>Rule 5: complete the upper row up to {@code playerCount + 4} Tribe
     *       cards, accounting for Events already placed there in step 4.</li>
     *   <li>Rule 6a: append Era I Building cards to the upper row, to the right
     *       of the Tribe cards.</li>
     * </ul>
     */
    private void dealInitialRows(int playerCount, List<BuildingCard> era1Buildings) {

        int lowerTarget = playerCount + 1;
        int lowerFilled = 0;
        while (lowerFilled < lowerTarget && !tribeDeck.isEmpty()) {
            Card card = tribeDeck.removeFirst();
            if (card instanceof EventCard) {
                upperRow.add(card);
            } else {
                lowerRow.add(card);
                lowerFilled++;
            }
        }

        int upperTribeTarget = playerCount + 4;
        while (upperRow.size() < upperTribeTarget && !tribeDeck.isEmpty()) {
            upperRow.add(tribeDeck.removeFirst());
        }

        upperRow.addAll(era1Buildings);

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
        List<Card> cards = new ArrayList<>();
        for (Card card : upperRow) {
            if (card.getCardType() == CardType.CHARACTER || card.getCardType() == CardType.EVENT) {
                cards.add(card);
            }
        }
        upperRow.removeAll(cards);
        lowerRow.addAll(cards);
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
        while (!buildingDeck.isEmpty()) {
            upperRow.add(buildingDeck.removeLast());
        }
    }
}
