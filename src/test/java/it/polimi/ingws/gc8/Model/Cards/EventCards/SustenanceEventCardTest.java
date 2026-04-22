package it.polimi.ingws.gc8.Model.Cards.EventCards;

import it.polimi.ingws.gc8.Model.Cards.CharacterCards.Artist;
import it.polimi.ingws.gc8.Model.Enum.Era;
import it.polimi.ingws.gc8.Model.Enum.EventType;
import it.polimi.ingws.gc8.Model.Enum.TotemColor;
import it.polimi.ingws.gc8.Model.Player;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SustenanceEventCardTest {

    @Test
    @DisplayName("Should correctly initialize SustenanceEventCard and verify all inherited fields")
    void testSustenanceEventCardInitialization() {
        String front = "sustenance_front.png";
        String back = "sustenance_back.png";
        Era era = Era.ERA_1;
        boolean isFinal = false;
        EventType eventType = EventType.sustenance;
        int prestigeMalus = 1;

        SustenanceEventCard sustenance = new SustenanceEventCard(front, back, era, isFinal, eventType, prestigeMalus);

        assertAll("Verify sustenance properties",
                () -> assertEquals(front, sustenance.getFront()),
                () -> assertEquals(back, sustenance.getBack()),
                () -> assertEquals(Era.ERA_1, sustenance.getEra()),
                () -> assertEquals(isFinal, sustenance.isFinal()),
                () -> assertEquals(eventType, sustenance.getEventType()),
                () -> assertEquals(prestigeMalus, sustenance.getPrestigeMalus())
        );
    }

    @Test
    @DisplayName("Should remove prestige points from players with zero food")
    void testResolveEvent() {
        int malus = 3;
        SustenanceEventCard sustenance = new SustenanceEventCard("f", "b", Era.ERA_1, false, EventType.sustenance, malus);

        Player p1 = new Player(0, "p1", TotemColor.RED);
        p1.addFood(5);
        p1.addPrestigePoints(10);

        Player p2 = new Player(1, "p2", TotemColor.BLUE);
        p2.addFood(0);
        p2.addPrestigePoints(10);

        Player p3 = new Player(2, "p3", TotemColor.BLUE);
        p2.addFood(0);
        p2.addPrestigePoints(10);

        for (int i = 0; i < 2; i++) {
            p2.getTribe().addCharacter(p2, new Artist("f", "b", Era.ERA_1, 0));
        }

        sustenance.resolveEvent(List.of(p1, p2, p3));

        assertEquals(10, p1.getPrestigePoints());
        assertEquals(14, p2.getPrestigePoints());
        assertEquals(0, p3.getPrestigePoints());
    }
}