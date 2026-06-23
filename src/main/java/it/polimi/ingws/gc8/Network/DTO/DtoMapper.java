package it.polimi.ingws.gc8.Network.DTO;

import it.polimi.ingws.gc8.Model.Board.OfferTile;
import it.polimi.ingws.gc8.Model.Cards.BuildingCard.BuildingCard;
import it.polimi.ingws.gc8.Model.Cards.Card;
import it.polimi.ingws.gc8.Model.Cards.CharacterCards.Builder;
import it.polimi.ingws.gc8.Model.Cards.CharacterCards.CharacterCard;
import it.polimi.ingws.gc8.Model.Cards.CharacterCards.Collector;
import it.polimi.ingws.gc8.Model.Cards.CharacterCards.Hunter;
import it.polimi.ingws.gc8.Model.Cards.CharacterCards.Inventor;
import it.polimi.ingws.gc8.Model.Cards.CharacterCards.Shaman;
import it.polimi.ingws.gc8.Model.Cards.EventCards.CavePaintingsEventCard;
import it.polimi.ingws.gc8.Model.Cards.EventCards.EventCard;
import it.polimi.ingws.gc8.Model.Cards.EventCards.HuntEventCard;
import it.polimi.ingws.gc8.Model.Cards.EventCards.RitualEventCard;
import it.polimi.ingws.gc8.Model.Cards.EventCards.SustenanceEventCard;
import it.polimi.ingws.gc8.Model.Enum.CardType;
import it.polimi.ingws.gc8.Model.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Static mapper from Model entities to network-serializable DTOs.
 * Keeps the Model layer free of any dependency on {@code Network.DTO}:
 * the mapping logic lives on the Network side, which is the consumer of DTOs.
 * <p>
 * The {@code Card → CardDTO} mapping uses dispatch via {@code instanceof} to
 * populate the specific fields of each subtype (Character / Building / Event)
 * in the fat record {@link CardDTO}.
 */
public final class DtoMapper {

    private DtoMapper() {}

    public static CardDTO toDTO(Card card) {
        if (card instanceof BuildingCard b) {
            return new CardDTO(
                    b.getFront(), b.getBack(), false, CardType.BUILDING, b.getEra(),
                    null, null, null, null, null, null, null, null,
                    b.getId(),
                    b.getFoodCost(),
                    b.getPrestigeBonus(),
                    b.getEffect() == null ? null : b.getEffect().getClass().getSimpleName(),
                    null, null, null, null, null, null, null, null
            );
        }
        if (card instanceof CharacterCard c) {
            Integer shamanIcons   = (c instanceof Shaman s)     ? s.getnIcons()          : null;
            Boolean hunterBonus   = (c instanceof Hunter h)     ? h.hasBonus()           : null;
            Integer builderFood   = (c instanceof Builder bd)   ? bd.getFoodBonus()      : null;
            Integer builderPP     = (c instanceof Builder bd)   ? bd.getPrestigePoints() : null;
            Integer collectorDisc = (c instanceof Collector co) ? co.getFoodDiscount()   : null;
            Integer inventorIcon  = (c instanceof Inventor in)  ? in.getIcon()           : null;
            return new CardDTO(
                    c.getFront(), c.getBack(), false, CardType.CHARACTER, c.getEra(),
                    c.getCharacterType(),
                    c.getNumberOfPlayers(),
                    shamanIcons,
                    hunterBonus,
                    builderFood,
                    builderPP,
                    collectorDisc,
                    inventorIcon,
                    null, null, null, null,
                    null, null, null, null, null, null, null, null
            );
        }
        if (card instanceof EventCard e) {
            Integer prestigeBonus  = null;
            Integer prestigeMalus  = null;
            Integer bonusArtists   = null;
            Integer malusArtists   = null;
            Integer bonusPoints    = null;
            Integer malusPoints    = null;
            if (e instanceof HuntEventCard h) {
                prestigeBonus = h.getPrestigeBonus();
            } else if (e instanceof SustenanceEventCard s) {
                prestigeMalus = s.getPrestigeMalus();
            } else if (e instanceof RitualEventCard r) {
                prestigeBonus = r.getPrestigeBonus();
                prestigeMalus = r.getPrestigeMalus();
            } else if (e instanceof CavePaintingsEventCard cp) {
                bonusArtists = cp.getBonusArtists();
                malusArtists = cp.getMalusArtists();
                bonusPoints  = cp.getBonusPoints();
                malusPoints  = cp.getMalusPoints();
            }
            return new CardDTO(
                    e.getFront(), e.getBack(), false, CardType.EVENT, e.getEra(),
                    null, null, null, null, null, null, null, null,
                    null, null, null, null,
                    e.getEventType(),
                    e.isFinal(),
                    prestigeBonus,
                    prestigeMalus,
                    bonusArtists,
                    malusArtists,
                    bonusPoints,
                    malusPoints
            );
        }
        throw new IllegalArgumentException("Unknown card subtype: " + card.getClass().getName());
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
        return new OfferTileDTO(tile.getId(), occupantId, tile.getImage());
    }
}
