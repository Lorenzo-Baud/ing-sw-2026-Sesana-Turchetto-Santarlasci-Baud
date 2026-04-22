package it.polimi.ingws.gc8.Model.Cards.EventCards;

import it.polimi.ingws.gc8.Model.Cards.CharacterCards.Artist;
import it.polimi.ingws.gc8.Model.Enum.*;
import it.polimi.ingws.gc8.Model.Player;
import it.polimi.ingws.gc8.Model.Tribe;
import it.polimi.ingws.gc8.Network.DTO.CardDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CavePaintingsEventCardTest {

    private CavePaintingsEventCard eventCard;
    private final int BONUS_ARTISTS = 3;
    private final int MALUS_ARTISTS = 2;
    private final int BONUS_POINTS = 5;
    private final int MALUS_POINTS = 10;

    @BeforeEach
    void setUp() {
        eventCard = new CavePaintingsEventCard(
                "front", "back", Era.ERA_1, false, EventType.sustenance,
                BONUS_ARTISTS, MALUS_ARTISTS, BONUS_POINTS, MALUS_POINTS
        );
    }

    @Test
    void testGetters() {
        assertEquals(BONUS_ARTISTS, eventCard.getBonusArtists());
        assertEquals(MALUS_ARTISTS, eventCard.getMalusArtists());
        assertEquals(BONUS_POINTS, eventCard.getBonusPoints());
        assertEquals(MALUS_POINTS, eventCard.getMalusPoints());
    }

    @Test
    void testResolveEvent_BonusTriggered() {
        Player player = new Player(0, "TestPlayer", TotemColor.RED);
        for (int i = 0; i < 3; i++) {
            player.getTribe().addCharacter(player, new Artist("f", "b", Era.ERA_1, 0));
        }

        int initialPoints = player.getPrestigePoints();
        eventCard.resolveEvent(List.of(player));

        assertEquals(initialPoints + 15, player.getPrestigePoints());
    }

    @Test
    void testResolveEvent_MalusTriggered() {
        Player player = new Player(0, "TestPlayer", TotemColor.RED);
        player.addPrestigePoints(20); // Give some points to allow subtraction

        // 0 artists is < MALUS_ARTISTS (2)
        int initialPoints = player.getPrestigePoints();
        eventCard.resolveEvent(List.of(player));

        assertEquals(initialPoints - MALUS_POINTS, player.getPrestigePoints());
    }

    @Test
    void testResolveEvent_NeutralZone() {
        Player player = new Player(0, "TestPlayer", TotemColor.RED);
        player.addPrestigePoints(20);

        for (int i = 0; i < 2; i++) {
            player.getTribe().addCharacter(player, new Artist("f", "b", Era.ERA_1, 0));
        }

        int initialPoints = player.getPrestigePoints();
        eventCard.resolveEvent(List.of(player));

        assertEquals(initialPoints, player.getPrestigePoints());
    }

    @Test
    void testResolveEvent_MultiplePlayers() {
        Player p1 = new Player(0, "TestPlayer", TotemColor.RED);
        for (int i = 0; i < 4; i++) {
            p1.getTribe().addCharacter(p1, new Artist("f", "b", Era.ERA_1, 0));
        }

        Player p2 = new Player(1, "TestPlayer2", TotemColor.BLUE);
        p2.addPrestigePoints(10);

        eventCard.resolveEvent(List.of(p1, p2));

        assertEquals(20, p1.getPrestigePoints()); // 4 * 5 = 20
        assertEquals(0, p2.getPrestigePoints());  // 10 - 10 = 0
    }

    @Test
    void testToDTO() {
        String front = "event_front.png";
        String back = "event_back.png";

        // Using CavePaintingsEventCard as a concrete implementation
        CavePaintingsEventCard card = new CavePaintingsEventCard(
                front, back, Era.ERA_1, false, EventType.sustenance,
                3, 2, 5, 10
        );

        CardDTO dto = card.toDTO();

        assertNotNull(dto);
        assertEquals(front, dto.front());
        assertEquals(back, dto.back());
        assertEquals(CardType.EVENT, dto.type());
        assertFalse(dto.isFlipped());
    }

}