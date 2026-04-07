package it.polimi.ingws.gc8.Model.Cards.EventCards;

import it.polimi.ingws.gc8.Model.Enum.Era;
import it.polimi.ingws.gc8.Model.Enum.EventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
}
