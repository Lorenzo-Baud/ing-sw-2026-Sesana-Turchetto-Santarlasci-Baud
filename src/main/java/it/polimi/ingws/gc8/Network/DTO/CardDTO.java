package it.polimi.ingws.gc8.Network.DTO;

import it.polimi.ingws.gc8.Model.Enum.CardType;
import it.polimi.ingws.gc8.Model.Enum.CharacterType;
import it.polimi.ingws.gc8.Model.Enum.Era;
import it.polimi.ingws.gc8.Model.Enum.EventType;

import java.io.Serializable;

/**
 * Data Transfer Object representing a card state for network communication.
 * <p>This class follows a <em>fat record</em> strategy: a single type carries
 * all the information needed for {@code Character}, {@code Building}, and
 * {@code Event} cards. Fields that do not apply to the actual card subtype
 * are set to {@code null}. The view checks {@link #type()} to decide which
 * fields are relevant.</p>
 *
 * @param front card front identifier (resource path or name)
 * @param back card back identifier
 * @param isFlipped {@code true} if the card is currently showing its back
 * @param type card type ({@code CHARACTER}, {@code BUILDING}, or {@code EVENT})
 * @param era era the card belongs to ({@code ERA_1}, {@code ERA_2}, {@code ERA_3})
 *
 * @param characterType character subtype ({@code null} if not a {@code CHARACTER})
 * @param numberOfPlayers minimum number of players required for the card to be in play
 * @param shamanIcons Shaman icons ({@code null} if not a Shaman card)
 * @param hunterBonus {@code true} if the Hunter has its bonus ({@code null} if not a Hunter card)
 * @param builderFoodBonus Builder food bonus ({@code null} if not a Builder card)
 * @param builderPrestigePoints Builder prestige points bonus
 * @param collectorFoodDiscount Collector food discount
 * @param inventorIcon Inventor icon ({@code null} if not an Inventor card)
 *
 * @param buildingId building identifier ({@code null} if not a {@code BUILDING})
 * @param buildingFoodCost food cost
 * @param buildingPrestigeBonus prestige points granted
 * @param buildingEffect symbolic effect name (for example, {@code "SetCompletionEffect"})
 *
 * @param eventType event type ({@code null} if not an {@code EVENT})
 * @param eventIsFinal {@code true} if this is the final event of the era
 * @param eventPrestigeBonus prestige points gained (Hunt / Ritual top)
 * @param eventPrestigeMalus prestige points lost (Sustenance / Ritual bottom)
 * @param eventBonusArtists artist threshold required to gain the bonus (CavePaintings)
 * @param eventMalusArtists artist threshold below which the malus applies
 * @param eventBonusPoints prestige points awarded for each artist above the threshold
 * @param eventMalusPoints prestige points lost below the threshold
 */
public record CardDTO(
        String front,
        String back,
        boolean isFlipped,
        CardType type,
        Era era,

        // CHARACTER
        CharacterType characterType,
        Integer numberOfPlayers,
        Integer shamanIcons,
        Boolean hunterBonus,
        Integer builderFoodBonus,
        Integer builderPrestigePoints,
        Integer collectorFoodDiscount,
        Integer inventorIcon,

        // BUILDING
        String buildingId,
        Integer buildingFoodCost,
        Integer buildingPrestigeBonus,
        String buildingEffect,

        // EVENT
        EventType eventType,
        Boolean eventIsFinal,
        Integer eventPrestigeBonus,
        Integer eventPrestigeMalus,
        Integer eventBonusArtists,
        Integer eventMalusArtists,
        Integer eventBonusPoints,
        Integer eventMalusPoints
) implements Serializable {}
