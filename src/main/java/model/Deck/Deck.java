package model.Deck;

import model.Deck.Cards.Card;

import java.util.ArrayDeque;
import java.util.Deque;

public class Deck {

    // Cards list
    Deque<Card> cards = new ArrayDeque<Card>();

    // Draws a random card from the deck, returns
    // null if the deck is empty
    public Card draw() {
        return cards.poll();
    }

    // Returns true if the deck is empty
    public boolean isEmpty() {
        return cards.isEmpty();
    }

    // Shuffles the deck. Returns:
    //  true: shuffle ended correctly
    //  false: error during shuffle
    public boolean shuffle() {
        return true;
    }

    // Returns deck size
    public int size() {
        return cards.size();
    }

}
