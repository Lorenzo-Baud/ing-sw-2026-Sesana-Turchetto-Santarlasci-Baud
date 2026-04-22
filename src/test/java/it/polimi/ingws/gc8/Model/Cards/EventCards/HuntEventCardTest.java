package it.polimi.ingws.gc8.Model.Cards.EventCards;

import it.polimi.ingws.gc8.Model.Cards.CharacterCards.Hunter;
import it.polimi.ingws.gc8.Model.Enum.Era;
import it.polimi.ingws.gc8.Model.Enum.EventType;
import it.polimi.ingws.gc8.Model.Enum.TotemColor;
import it.polimi.ingws.gc8.Model.Player;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class HuntEventCardTest {
    @Test
    @DisplayName("Should correctly initialize HuntEventCard and verify all inherited fields")
    void TestHuntEventCardInitialization() {
        String front = "hunt_front.png";
        String back =  "hunt_back.png";
        Era era = Era.ERA_1;
        boolean isFinal = false;
        EventType eventType = EventType.hunt;
        int prestigeBonus = 1;

        HuntEventCard hunt = new HuntEventCard(front, back, era, isFinal, eventType, prestigeBonus);

        assertAll("Verify Hunt properties",
                () -> assertEquals(front, hunt.getFront(), "Front image mismatch"),
                () -> assertEquals(back, hunt.getBack(), "Back image mismatch"),
                () -> assertEquals(Era.ERA_1, hunt.getEra(), "Era mismatch"),
                () -> assertEquals(isFinal, hunt.isFinal(), "isFinal mismatch"),
                () -> assertEquals(eventType, hunt.getEventType(), "eventType must be cavePaintings"),
                () -> assertEquals(prestigeBonus, hunt.getPrestigeBonus(), "prestigeBonus mismatch")
        );
    }

    @Test
    void testResolveEvent_WithHunters() {
        int prestigeBonus = 3;
        HuntEventCard eventCard = new HuntEventCard(
                "front", "back", Era.ERA_1, false, EventType.hunt, prestigeBonus
        );

        Player player = new Player(0, "TestPlayer", TotemColor.RED);
        // Add 2 Hunters to the tribe
        player.getTribe().addCharacter(player, new Hunter("f1", "b1", Era.ERA_1, 0, false));
        player.getTribe().addCharacter(player, new Hunter("f2", "b2", Era.ERA_1, 0, false));

        int initialFood = player.getFood();
        int initialPoints = player.getPrestigePoints();

        eventCard.resolveEvent(List.of(player));

        // Expected: 2 hunters -> +2 food, + (2 * 3) = 6 prestige points
        assertEquals(initialFood + 2, player.getFood());
        assertEquals(initialPoints + 6, player.getPrestigePoints());
    }

    @Test
    void testResolveEvent_NoHunters() {
        int prestigeBonus = 5;
        HuntEventCard eventCard = new HuntEventCard(
                "front", "back", Era.ERA_1, false, EventType.hunt, prestigeBonus
        );

        Player player = new Player(0, "TestPlayer", TotemColor.RED);
        int initialFood = player.getFood();
        int initialPoints = player.getPrestigePoints();

        eventCard.resolveEvent(List.of(player));

        // Expected: 0 hunters -> no changes
        assertEquals(initialFood, player.getFood());
        assertEquals(initialPoints, player.getPrestigePoints());
    }

    @Test
    void testResolveEvent_MultiplePlayers() {
        HuntEventCard eventCard = new HuntEventCard(
                "front", "back", Era.ERA_1, false, EventType.hunt, 2
        );

        Player p1 = new Player(0, "TestPlayer", TotemColor.RED);
        p1.getTribe().addCharacter(p1, new Hunter("f1", "b1", Era.ERA_1, 0, false));

        Player p2 = new Player(0, "TestPlayer", TotemColor.RED);

        eventCard.resolveEvent(List.of(p1, p2));

        assertEquals(1, p1.getFood());
        assertEquals(2, p1.getPrestigePoints());
        assertEquals(0, p2.getFood());
        assertEquals(0, p2.getPrestigePoints());
    }

}
