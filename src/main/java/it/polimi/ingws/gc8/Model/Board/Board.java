package it.polimi.ingws.gc8.Model.Board;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.polimi.ingws.gc8.Model.Cards.BuildingCard.BuildingCard;
import it.polimi.ingws.gc8.Model.Cards.Card;
import it.polimi.ingws.gc8.Model.Cards.CharacterCard;
import it.polimi.ingws.gc8.Model.Cards.CharacterCards.*;
import it.polimi.ingws.gc8.Model.Cards.EventCard;
import it.polimi.ingws.gc8.Model.Enum.Era;
import it.polimi.ingws.gc8.Model.Enum.EventType;
import it.polimi.ingws.gc8.Model.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Board {
    private List<Card> tribeDeck;
    private List<Card> lowerRow;
    private List<Card> upperRow;
    private TurnOrderTrack turnOrderTrack;
    private List<Card> era2BuildingDeck;
    private List<Card> era3BuildingDeck;
    private OfferTrack offerTrack;

    public Board(int playerCount) {
        this.tribeDeck = new ArrayList<>();
        this.lowerRow = new ArrayList<>();
        this.upperRow = new ArrayList<>();
        this.era2BuildingDeck = new ArrayList<>();
        this.era3BuildingDeck = new ArrayList<>();
        this.offerTrack = new OfferTrack(playerCount);
    }

    public Board(List<Card> tribeDeck, List<Card> lowerRow, List<Card> upperRow,
                 TurnOrderTrack turnOrderTrack, List<Card> era2BuildingDeck,
                 List<Card> era3BuildingDeck, OfferTrack offerTrack) {
        this.tribeDeck = tribeDeck;
        this.lowerRow = lowerRow;
        this.upperRow = upperRow;
        this.turnOrderTrack = turnOrderTrack;
        this.era2BuildingDeck = era2BuildingDeck;
        this.era3BuildingDeck = era3BuildingDeck;
        this.offerTrack = offerTrack;
    }

    // Parses JSON file to get tribe cards data
    public void loadTribeDeck() {

        // Character cards parsing
        // *************************************************************************
        ObjectMapper mapper = new ObjectMapper();

        try {
            JsonNode root = mapper.readTree(new File("src/main/resources/character_cards.json"));

            for (JsonNode node : root) {

                // Field extraction
                String type = node.get("type").asText();
                String front = node.get("front").asText();
                String back = node.get("back").asText();
                int eraNum = node.get("era").asInt();
                int players = node.get("number_of_players").asInt();
                Era era = Era.values()[eraNum - 1];

                switch (type) {
                    case "artist":
                        tribeDeck.add(new Artist(front, back, era, players));
                        break;

                    case "inventor":
                        int icon = node.get("icon").asInt();
                        tribeDeck.add(new Inventor(front, back, era, players, icon));
                        break;

                    case "collector":
                        int discount = node.get("foodDiscount").asInt();
                        tribeDeck.add(new Collector(front, back, era, players, discount));
                        break;

                    case "shaman":
                        int iconsCount = node.get("icons").asInt();
                        tribeDeck.add(new Shaman(front, back, era, players, iconsCount));
                        break;

                    case "hunter":
                        boolean bonus = node.get("bonus").asBoolean();
                        tribeDeck.add(new Hunter(front, back, era, players, bonus));
                        break;

                    case "builder":
                        int pPoints = node.get("prestigePoints").asInt();
                        int fBonus = node.get("foodBonus").asInt();
                        tribeDeck.add(new Builder(front, back, era, players, pPoints, fBonus));
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // *************************************************************************

        // Event cards parsing
        // *************************************************************************
        try {
            JsonNode root = mapper.readTree(new File("src/main/resources/event_cards.json"));

            for (JsonNode node : root) {

                // Field extraction
                String front = node.get("front").asText();
                String back = node.get("back").asText();
                int eraNum = node.get("era").asInt();
                boolean isFinal = node.get("isFinal").asBoolean();
                Era era = Era.values()[eraNum - 1];
                String type = node.get("type").asText();

                // Optional variables
                int prestigeBonus;
                int prestigeMalus;
                int bonusArists;
                int malusArists;
                int bonusPoints;
                int malusPoints;

                switch (type) {
                    case "sustenance":
                        prestigeMalus = node.get("prestigeMalus").asInt();
                        tribeDeck.add(new EventCard(front, back, era, isFinal, EventType.sustenance, players -> {
                            for (Player player : players) {

                                // Calculates total food cost
                                int foodCost = player.getTribe().getCharacters().size();
                                foodCost = foodCost - (player.getTribe().countByType(Collector.class) * 3);

                                // Removes food to player
                                if(foodCost <= player.getFood()) {
                                    player.removeFood(foodCost);
                                } else {

                                    // Calculates how many prestige points to remove
                                    player.removePrestigePoints((foodCost - player.getFood()) * prestigeMalus);
                                    player.removeFood(player.getFood());

                                }

                            }
                        }));
                        break;

                    case "hunt":
                        prestigeBonus = node.get("prestigeBonus").asInt();
                        tribeDeck.add(new EventCard(front, back, era, isFinal, EventType.hunt, players -> {
                            for (Player player : players) {

                            }
                        }));
                        break;

                    case "cavePaintings":
                        bonusArists = node.get("bonusArtists").asInt();
                        malusArists = node.get("malusArtists").asInt();
                        bonusPoints = node.get("bonusPoints").asInt();
                        malusPoints = node.get("malusPoints").asInt();
                        tribeDeck.add(new EventCard(front, back, era, isFinal, EventType.cavePaintings, players -> {
                            for (Player player : players) {

                            }
                        }));
                        break;
                    case "ritual":
                        prestigeBonus = node.get("prestigeBonus").asInt();
                        prestigeMalus = node.get("prestigeMalus").asInt();
                        tribeDeck.add(new EventCard(front, back, era, isFinal, EventType.ritual, null));
                        break;
                }


            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        // *************************************************************************

        // Deck shuffle
        // *************************************************************************
        // Shuffles deck
        java.util.Collections.shuffle(tribeDeck);

        // Orders deck by era
        tribeDeck.sort((c1, c2) -> {
            if (c1.getEra() == c2.getEra()) {
                return 0;
            }
            return c1.getEra().ordinal() < c2.getEra().ordinal() ? -1 : 1;
        });
        // *************************************************************************


    }

    public List<Card> getTribeDeck() {
        return tribeDeck;
    }

    public void setTribeDeck(List<Card> tribeDeck) {
        this.tribeDeck = tribeDeck;
    }

    public List<Card> getLowerRow() {
        return lowerRow;
    }

    public void setLowerRow(List<Card> lowerRow) {
        this.lowerRow = lowerRow;
    }

    public List<Card> getUpperRow() {
        return upperRow;
    }

    public void setUpperRow(List<Card> upperRow) {
        this.upperRow = upperRow;
    }

    public TurnOrderTrack getTurnOrderTrack() {
        return turnOrderTrack;
    }

    public void setTurnOrderTrack(TurnOrderTrack turnOrderTrack) {
        this.turnOrderTrack = turnOrderTrack;
    }

    public List<Card> getEra2BuildingDeck() {
        return era2BuildingDeck;
    }

    public void setEra2BuildingDeck(List<Card> era2BuildingDeck) {
        this.era2BuildingDeck = era2BuildingDeck;
    }

    public List<Card> getEra3BuildingDeck() {
        return era3BuildingDeck;
    }

    public void setEra3BuildingDeck(List<Card> era3BuildingDeck) {
        this.era3BuildingDeck = era3BuildingDeck;
    }

    public OfferTrack getOfferTrack() {
        return offerTrack;
    }

    public void setOfferTrack(OfferTrack offerTrack) {
        this.offerTrack = offerTrack;
    }

    public void setupInitialRows(int PlayerCount, List<BuildingCard> era1Building){ //fare check se va bene
        for (int i = 0; i < PlayerCount + 1; i++) {
            lowerRow.add(era1Building.get(i));
        }
        for (int i = PlayerCount + 1; i < PlayerCount * 2 + 2; i++) {
            upperRow.add(era1Building.get(i));
        }
    }

    public Card getLowerRowCard(int index) {
        if (index >= 0 && index < lowerRow.size()) {
            return lowerRow.get(index);
        }
        throw new IndexOutOfBoundsException("Invalid index for lower row: " + index);
    }

    public Card getUpperRowCard(int index) {
        if (index >= 0 && index < upperRow.size()) {
            return upperRow.get(index);
        }
        throw new IndexOutOfBoundsException("Invalid index for upper row: " + index);
    }

    public Card removeLowerRowCard(int index) {
        if (index >= 0 && index < lowerRow.size()) {
            return lowerRow.remove(index);
        }
        throw new IndexOutOfBoundsException("Invalid index for lower row: " + index);
    }

    public Card removeUpperRowCard(int index) {
        if (index >= 0 && index < upperRow.size()) {
            return upperRow.remove(index);
        }
        throw new IndexOutOfBoundsException("Invalid index for upper row: " + index);
    }

    public List<EventCard> getLowerRowEvents() {
        List<EventCard> events = new ArrayList<>();
        for (Card card : lowerRow) {
            if (card instanceof EventCard) {
                events.add((EventCard) card);
            }
        }
        return events;
    }

    public void discardLowerRowTribeCards() {
        lowerRow.removeIf(card -> card instanceof CharacterCard || card instanceof EventCard);
    }

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

    public List<Card> refillUpperRow(int playerCount) { //fare check se va bene
        List<Card> newTribeCards = new ArrayList<>();
        for (int i = 0; i < playerCount + 1; i++) {
            if (!tribeDeck.isEmpty()) {
                Card card = (Card) tribeDeck.remove(tribeDeck.size() - 1);
                upperRow.add(card);
                newTribeCards.add(card);
            }
        }
        return newTribeCards;
    }

    public void discardLowerRowBuilding(){
        lowerRow.removeIf(card -> card instanceof BuildingCard);
    }

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

    public void revealEraBuildings(int era) { //fare check se va bene
        List<Card> buildingDeck = (era == 2) ? era2BuildingDeck : era3BuildingDeck;
        for (int i = 0; i < 4; i++) {
            if (!buildingDeck.isEmpty()) {
                lowerRow.add(buildingDeck.remove(buildingDeck.size() - 1));
            }
        }
    }
}
