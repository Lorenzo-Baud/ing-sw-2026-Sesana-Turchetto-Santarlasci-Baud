package it.polimi.ingws.gc8.Model.Cards.EventCards;

import it.polimi.ingws.gc8.Model.Enum.Era;
import it.polimi.ingws.gc8.Model.Enum.EventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class RitualEventCardTest {
    @Test
    @DisplayName("Should correctly initialize RitualEventCard and verify all inherited fields")
    void TestRitualEventCardInitialization(){

        String front = "ritual_front.png";
        String back = "ritual_back.png";
        Era era = Era.ERA_1;
        boolean isFinal = false;
        EventType eventType = EventType.ritual;
        int prestigeBonus = 5;
        int prestigeMalus = 3;

        RitualEventCard ritual = new RitualEventCard(front, back, era, isFinal, eventType, prestigeBonus, prestigeMalus );


        assertAll("Verify ritual properties",
                () -> assertEquals(front, ritual.getFront(), "Front image mismatch"),
                () -> assertEquals(back, ritual.getBack(), "Back image mismatch"),
                () -> assertEquals(Era.ERA_1, ritual.getEra(), "Era mismatch"),
                () -> assertEquals(isFinal, ritual.isFinal(), "isFinal mismatch"),
                () -> assertEquals(eventType, ritual.getEventType(), "eventType must be cavePaintings"),
                () -> assertEquals(prestigeBonus, ritual.getPrestigeBonus(), "prestigeBonus mismatch"),
                () -> assertEquals(prestigeMalus, ritual.getPrestigeMalus(), "prestigeMalus mismatch")
        );
    }
}
