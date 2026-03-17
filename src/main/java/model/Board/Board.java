package model.Board;

import model.Deck.Cards.BuildingCard;
import model.Deck.Cards.Card;
import model.Deck.Cards.TribeCards.EventCard;
import model.Deck.Cards.TribeCards.TribeCard;
import model.Deck.Deck;

import java.util.ArrayList;
import java.util.List;

public class Board {
    private Deck tribeDeck;
    private List<Card> lowerRow;
    private List<Card> upperRow;
    private TurnOrderTrack turnOrderTrack;
    private Deck era2BuildingDeck;
    private Deck era3BuildingDeck;
    private OfferTrack offerTrack;

    public Board(Deck tribeDeck, List lowerRow, List<Card> upperRow,
                 TurnOrderTrack turnOrderTrack, Deck era2BuildingDeck,
                 Deck era3BuildingDeck, OfferTrack offerTrack) {
        this.tribeDeck = tribeDeck;
        this.lowerRow = lowerRow;
        this.upperRow = upperRow;
        this.turnOrderTrack = turnOrderTrack;
        this.era2BuildingDeck = era2BuildingDeck;
        this.era3BuildingDeck = era3BuildingDeck;
        this.offerTrack = offerTrack;
    }

    public Deck getTribeDeck() {
        return tribeDeck;
    }

    public void setTribeDeck(Deck tribeDeck) {
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

    public Deck getEra2BuildingDeck() {
        return era2BuildingDeck;
    }

    public void setEra2BuildingDeck(Deck era2BuildingDeck) {
        this.era2BuildingDeck = era2BuildingDeck;
    }

    public Deck getEra3BuildingDeck() {
        return era3BuildingDeck;
    }

    public void setEra3BuildingDeck(Deck era3BuildingDeck) {
        this.era3BuildingDeck = era3BuildingDeck;
    }

    public OfferTrack getOfferTrack() {
        return offerTrack;
    }

    public void setOfferTrack(OfferTrack offerTrack) {
        this.offerTrack = offerTrack;
    }

    public void setupInitialRows(int PlayerCount, List<model.Deck.Cards.BuildingCard> era1Building){ //fare check se va bene
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
        lowerRow.removeIf(card -> card instanceof TribeCard);
    }

    public void moveUpperRowTribeCardsToLower() {
        List<Card> tribeCards = new ArrayList<>();
        for (Card card : upperRow) {
            if (card instanceof TribeCard) {
                tribeCards.add(card);
            }
        }
        upperRow.removeAll(tribeCards);
        lowerRow.addAll(tribeCards);
    }

    public List<TribeCard> refillUpperRow(int playerCount) { //fare check se va bene
        List<TribeCard> newTribeCards = new ArrayList<>();
        for (int i = 0; i < playerCount + 1; i++) {
            if (!tribeDeck.isEmpty()) {
                TribeCard card = (TribeCard) tribeDeck.draw();
                upperRow.add(card);
                newTribeCards.add(card);
            }
        }
        return newTribeCards;
    }

    public void discardLowerRowBuilding(){
        lowerRow.removeIf(card -> card instanceof model.Deck.Cards.BuildingCard);
    }

    public void moveUpperRowBuildingToLower() {
        List<Card> buildingCards = new ArrayList<>();
        for (Card card : upperRow) {
            if (card instanceof model.Deck.Cards.BuildingCard) {
                buildingCards.add(card);
            }
        }
        upperRow.removeAll(buildingCards);
        lowerRow.addAll(buildingCards);
    }

    public void revealEraBuildings(int era) { //fare check se va bene
        Deck buildingDeck = (era == 2) ? era2BuildingDeck : era3BuildingDeck;
        for (int i = 0; i < 4; i++) {
            if (!buildingDeck.isEmpty()) {
                lowerRow.add(buildingDeck.draw());
            }
        }
    }
}
