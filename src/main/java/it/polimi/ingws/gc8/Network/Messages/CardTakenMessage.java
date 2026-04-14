package it.polimi.ingws.gc8.Network.Messages;

import it.polimi.ingws.gc8.Network.DTO.CardDTO;

/**
 * Specific message for when a player takes a card.
 * Useful for triggering animations on the client.
 */
public class CardTakenMessage implements NetworkMessage {
    private static final long serialVersionUID = 1L;

    private final int playerId;
    private final CardDTO card;

    public CardTakenMessage(int playerId, CardDTO card) {
        this.playerId = playerId;
        this.card = card;
    }

    public int getPlayerId() {
        return playerId;
    }

    public CardDTO getCard() {
        return card;
    }
}