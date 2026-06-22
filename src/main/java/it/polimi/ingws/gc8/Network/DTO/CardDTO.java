package it.polimi.ingws.gc8.Network.DTO;

import it.polimi.ingws.gc8.Model.Enum.CardType;
import it.polimi.ingws.gc8.Model.Enum.CharacterType;
import it.polimi.ingws.gc8.Model.Enum.Era;
import it.polimi.ingws.gc8.Model.Enum.EventType;

import java.io.Serializable;

/**
 * Data Transfer Object representing a Card's state for network communication.
 *
 * Strategia "fat record": un unico tipo trasporta tutte le informazioni
 * specifiche di Character / Building / Event. I campi non pertinenti al
 * sottotipo della carta sono {@code null}. La View consulta {@link #type()}
 * per decidere quali campi leggere.
 *
 * @param front      identificatore del fronte (path/risorsa o nome)
 * @param back       identificatore del retro
 * @param isFlipped  true se la carta mostra il retro
 * @param type       tipo di carta (CHARACTER / BUILDING / EVENT)
 * @param era        era a cui appartiene la carta (ERA_1 / ERA_2 / ERA_3)
 *
 * @param characterType         tipo personaggio (null se non CHARACTER)
 * @param numberOfPlayers       n. minimo giocatori per cui la carta entra in gioco
 * @param shamanIcons           icone Shaman (null se non SHAMAN)
 * @param hunterBonus           true se Hunter con bonus (null se non HUNTER)
 * @param builderFoodBonus      bonus food del Builder (null se non BUILDER)
 * @param builderPrestigePoints PP del Builder
 * @param collectorFoodDiscount sconto food del Collector
 * @param inventorIcon          icona Inventor (null se non INVENTOR)
 *
 * @param buildingId             id edificio (null se non BUILDING)
 * @param buildingFoodCost       costo in food
 * @param buildingPrestigeBonus  PP forniti
 * @param buildingEffect         nome simbolico dell'effetto (es. "SetCompletionEffect")
 *
 * @param eventType            tipo evento (null se non EVENT)
 * @param eventIsFinal         true se evento finale dell'era
 * @param eventPrestigeBonus   PP guadagnati (Hunt / Ritual top)
 * @param eventPrestigeMalus   PP persi (Sustenance / Ritual bottom)
 * @param eventBonusArtists    soglia di Artist per ottenere bonus (CavePaintings)
 * @param eventMalusArtists    soglia di Artist sotto la quale scatta il malus
 * @param eventBonusPoints     PP per ogni Artist sopra soglia
 * @param eventMalusPoints     PP persi sotto soglia
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
