package it.polimi.ingws.gc8.Model.Board;

import it.polimi.ingws.gc8.Model.Deck.Cards.BuildingCard;
import it.polimi.ingws.gc8.Model.Deck.Cards.Card;
import it.polimi.ingws.gc8.Model.Deck.Cards.EventCard;
import it.polimi.ingws.gc8.Model.Deck.Cards.CharacterCard;

import java.util.ArrayList;
import java.util.List;

public class Board {
    private List<CharacterCard> tribeDeck;
    private List<Card> lowerRow;
    private List<Card> upperRow;
    private TurnOrderTrack turnOrderTrack;
    private List<BuildingCard> era2BuildingDeck;
    private List<BuildingCard> era3BuildingDeck;
    private OfferTrack offerTrack;

    // Costruttore
    public Board(int playerCount, List<CharacterCard> allTribeCards,
                 List<BuildingCard> era1Buildings,
                 List<BuildingCard> era2Buildings,
                 List<BuildingCard> era3Buildings) {

        this.lowerRow = new ArrayList<>();
        this.upperRow = new ArrayList<>();
        this.offerTrack = new OfferTrack(playerCount);
        this.turnOrderTrack = new TurnOrderTrack(playerCount);
        this.tribeDeck = new ArrayList<>(allTribeCards);
        this.era2BuildingDeck = new ArrayList<>(era2Buildings);
        this.era3BuildingDeck = new ArrayList<>(era3Buildings);

        setupInitialRows(playerCount, era1Buildings);
    }

    // Getters
    public List<CharacterCard> getTribeDeck() { return tribeDeck; }
    public List<Card> getLowerRow() { return lowerRow; }
    public List<Card> getUpperRow() { return upperRow; }
    public TurnOrderTrack getTurnOrderTrack() { return turnOrderTrack; }
    public OfferTrack getOfferTrack() { return offerTrack; }
    public List<BuildingCard> getEra2BuildingList() { return era2BuildingDeck; }
    public List<BuildingCard> getEra3BuildingList() { return era3BuildingDeck; }

    // Setup iniziale — regole pag. 2, passi 4-6
    public void setupInitialRows(int playerCount, List<BuildingCard> era1Buildings) {

        // passo 4: pesca playerCount+1 carte per la fila inferiore
        // se si pesca un Evento → va in superiore, si continua a pescare
        int lowerFilled = 0;
        while (lowerFilled < playerCount + 1 && !tribeDeck.isEmpty()) {
            Card card = tribeDeck.remove(0);
            if (card instanceof EventCard) {
                upperRow.add(card); // evento → fila superiore, non conta per il lower
            } else {
                lowerRow.add(card);
                lowerFilled++;
            }
        }

        // passo 5: pesca playerCount+4 carte per la fila superiore
        // considera gli eventi già presenti nella fila superiore
        int upperNeeded = (playerCount + 4) - upperRow.size();
        for (int i = 0; i < upperNeeded; i++) {
            if (!tribeDeck.isEmpty()) {
                upperRow.add(tribeDeck.remove(0));
            }
        }

        // passo 6a: aggiunge edifici Era I in fila superiore a destra delle carte tribù
        upperRow.addAll(era1Buildings);
    }

    // Accesso e rimozione carte dalle file
    public Card getLowerRowCard(int index) {
        if (index >= 0 && index < lowerRow.size()) return lowerRow.get(index);
        throw new IndexOutOfBoundsException("Invalid index for lower row: " + index);
    }

    public Card getUpperRowCard(int index) {
        if (index >= 0 && index < upperRow.size()) return upperRow.get(index);
        throw new IndexOutOfBoundsException("Invalid index for upper row: " + index);
    }

    public Card removeLowerRowCard(int index) {
        if (index >= 0 && index < lowerRow.size()) return lowerRow.remove(index);
        throw new IndexOutOfBoundsException("Invalid index for lower row: " + index);
    }

    public Card removeUpperRowCard(int index) {
        if (index >= 0 && index < upperRow.size()) return upperRow.remove(index);
        throw new IndexOutOfBoundsException("Invalid index for upper row: " + index);
    }

    // Fine round — regole pag. 6, passi 1-4
    // chiamare in questo ordine:
    // 1. getLowerRowEvents() → risolvi eventi
    // 2. discardLowerRowTribeCards()
    // 3. moveUpperRowTribeCardsToLower()
    // 4. refillUpperRow(playerCount)

    // passo 1: restituisce gli eventi in fila inferiore da risolvere
    public List<EventCard> getLowerRowEvents() {
        List<EventCard> events = new ArrayList<>();
        for (Card card : lowerRow) {
            if (card instanceof EventCard) events.add((EventCard) card);
        }
        return events;
    }

    // passo 2: scarta Personaggio ed Evento dalla fila inferiore
    // le carte Edificio rimangono al loro posto
    public void discardLowerRowTribeCards() {
        lowerRow.removeIf(card -> card instanceof CharacterCard || card instanceof EventCard);
    }

    // passo 3: sposta Personaggio ed Evento dalla fila superiore alla inferiore
    // le carte Edificio rimangono al loro posto
    public void moveUpperRowTribeCardsToLower() {
        List<Card> toMove = new ArrayList<>();
        for (Card card : upperRow) {
            if (card instanceof CharacterCard || card instanceof EventCard) {
                toMove.add(card);
            }
        }
        upperRow.removeAll(toMove);
        lowerRow.addAll(toMove);
    }

    // passo 4: ripristina fila superiore pescando playerCount+4 carte
    public List<CharacterCard> refillUpperRow(int playerCount) {
        List<CharacterCard> newCards = new ArrayList<>();
        int needed = playerCount + 4;

        for (int i = 0; i < needed; i++) {
            if (!tribeDeck.isEmpty()) {
                Card card = tribeDeck.remove(0);
                upperRow.add(card);
                if (!(card instanceof EventCard)) {
                    newCards.add((CharacterCard) card);
                }
                // gli EventCard vengono aggiunti alla upperRow ma non restituiti
            }
        }
        return newCards;
    }

    // Nuova era — regole pag. 7, passi 1-3

    // passo 1: solo Era III → scarta edifici dalla fila inferiore
    public void discardLowerRowBuildings() {
        lowerRow.removeIf(card -> card instanceof BuildingCard);
    }

    // passo 2: sposta edifici dalla fila superiore alla fila inferiore
    public void moveUpperRowBuildingsToLower() {
        List<Card> buildingCards = new ArrayList<>();
        for (Card card : upperRow) {
            if (card instanceof BuildingCard) buildingCards.add(card);
        }
        upperRow.removeAll(buildingCards);
        lowerRow.addAll(buildingCards);
    }

    // passo 3: rivela edifici della nuova era — gestisce i 3 passi nell'ordine corretto
    public void revealEraBuildings(int era) {
        // solo Era III: scarta edifici dalla fila inferiore
        if (era == 3) {
            discardLowerRowBuildings();
        }
        // sposta edifici Era precedente dalla superiore all'inferiore
        moveUpperRowBuildingsToLower();

        // rivela nuovi edifici in fila superiore
        List<BuildingCard> buildingList = (era == 2) ? era2BuildingDeck : era3BuildingDeck;
        upperRow.addAll(buildingList);
        buildingList.clear(); // svuota la lista dopo aver rivelato
    }
}