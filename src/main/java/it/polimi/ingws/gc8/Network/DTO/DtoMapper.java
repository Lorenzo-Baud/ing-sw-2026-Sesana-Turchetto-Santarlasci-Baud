package it.polimi.ingws.gc8.Network.DTO;

import it.polimi.ingws.gc8.Model.Board.OfferTile;
import it.polimi.ingws.gc8.Model.Cards.BuildingCard.BuildingCard;
import it.polimi.ingws.gc8.Model.Cards.Card;
import it.polimi.ingws.gc8.Model.Cards.CharacterCards.CharacterCard;
import it.polimi.ingws.gc8.Model.Cards.EventCards.EventCard;
import it.polimi.ingws.gc8.Model.Enum.CardType;
import it.polimi.ingws.gc8.Model.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Static mapper from Model entities to network-serializable DTOs.
 * Keeps the Model layer free of any dependency on {@code Network.DTO}:
 * the mapping logic lives on the Network side, which is the consumer of DTOs.
 */
public final class DtoMapper {

    private DtoMapper() {}

    public static CardDTO toDTO(Card card) {
        CardType type;
        if (card instanceof BuildingCard) {
            type = CardType.BUILDING;
        } else if (card instanceof CharacterCard) {
            type = CardType.CHARACTER;
        } else if (card instanceof EventCard) {
            type = CardType.EVENT;
        } else {
            throw new IllegalArgumentException("Unknown card subtype: " + card.getClass().getName());
        }
        return new CardDTO(card.getFront(), card.getBack(), false, type);
    }

    public static PlayerDTO toDTO(Player player) {
        List<CardDTO> tribeDTO = new ArrayList<>();
        for (Card c : player.getTribe().getCharacters()) {
            tribeDTO.add(toDTO(c));
        }
        for (Card c : player.getTribe().getBuildings()) {
            tribeDTO.add(toDTO(c));
        }
        return new PlayerDTO(
                player.getPlayerId(),
                player.getNickname(),
                player.getTotemColor(),
                player.getFood(),
                player.getPrestigePoints(),
                tribeDTO
        );
    }

    public static OfferTileDTO toDTO(OfferTile tile) {
        int occupantId = tile.getOccupant() != null ? tile.getOccupant().getPlayerId() : -1;
        return new OfferTileDTO(tile.getId(), occupantId);
    }
}
