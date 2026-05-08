package it.polimi.ingws.gc8.Model.Cards.EventCards;

import it.polimi.ingws.gc8.Model.Enum.Era;
import it.polimi.ingws.gc8.Model.Enum.EventType;
import it.polimi.ingws.gc8.Model.Enum.TotemColor;
import it.polimi.ingws.gc8.Model.Player;
import it.polimi.ingws.gc8.Model.Cards.CharacterCards.Shaman;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RitualEventCardTest {

    @Test
    void testGetters() {
        RitualEventCard card = new RitualEventCard("f", "b", Era.ERA_1, false, EventType.ritual, 10, 5);
        assertEquals(10, card.getPrestigeBonus());
        assertEquals(5, card.getPrestigeMalus());
    }

    @Test
    void testResolveEvent_ClearMaxAndMin() {
        RitualEventCard card = new RitualEventCard("f", "b", Era.ERA_1, false, EventType.ritual, 10, 5);

        Player p1 = new Player(0, "Rich", TotemColor.RED);
        p1.getTribe().addCharacter(p1, new Shaman("f", "b", Era.ERA_1, 2, 0));

        Player p2 = new Player(1, "Middle", TotemColor.BLUE);
        p2.getTribe().addCharacter(p2, new Shaman("f", "b", Era.ERA_1, 1, 0));

        Player p3 = new Player(2, "Poor", TotemColor.RED);
        p3.addPrestigePoints(10);

        card.resolveEvent(List.of(p1, p2, p3));

        assertEquals(10, p1.getPrestigePoints()); // Bonus only
        assertEquals(0, p2.getPrestigePoints());  // No bonus, no malus
        assertEquals(5, p3.getPrestigePoints());  // 10 - 5 (Malus)
    }

    @Test
    void testResolveEvent_AllEqual() {
        RitualEventCard card = new RitualEventCard("f", "b", Era.ERA_1, false, EventType.ritual, 10, 5);

        Player p1 = new Player(0, "P1", TotemColor.RED);
        p1.addPrestigePoints(10);
        Player p2 = new Player(1, "P2", TotemColor.BLUE);
        p2.addPrestigePoints(10);

        // Both have 0 shaman icons: both are max and both are min
        card.resolveEvent(List.of(p1, p2));

        // Result: 10 (initial) + 10 (bonus) - 5 (malus) = 15
        assertEquals(15, p1.getPrestigePoints());
        assertEquals(15, p2.getPrestigePoints());

        RitualEventCard card2 = new RitualEventCard("f", "b", Era.ERA_1, false, EventType.ritual, 10, 5);
        card2.resolveEvent(null);
        card2.resolveEvent(List.of());


    }

    @Test
    void testResolveEvent_MultipleTies() {
        RitualEventCard card = new RitualEventCard("f", "b", Era.ERA_1, false, EventType.ritual, 10, 5);

        Player p1 = new Player(0, "Max1", TotemColor.RED);
        p1.getTribe().addCharacter(p1, new Shaman("f", "b", Era.ERA_1, 3, 0));

        Player p2 = new Player(1, "Max2", TotemColor.BLUE);
        p2.getTribe().addCharacter(p2, new Shaman("f", "b", Era.ERA_1, 10, 0));

        Player p3 = new Player(2, "Min1", TotemColor.RED);
        p3.addPrestigePoints(10);

        Player p4 = new Player(3, "Min2", TotemColor.YELLOW);
        p4.addPrestigePoints(10);

        card.resolveEvent(List.of(p1, p2, p3, p4));

        assertEquals(0, p1.getPrestigePoints()); // Only bonus
        assertEquals(10, p2.getPrestigePoints()); // Only bonus
        assertEquals(5, p3.getPrestigePoints());  // 10 - 5 (Malus)
        assertEquals(5, p4.getPrestigePoints());  // 10 - 5 (Malus)
    }
}