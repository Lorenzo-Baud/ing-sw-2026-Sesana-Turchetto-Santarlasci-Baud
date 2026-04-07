package it.polimi.ingws.gc8.Model.Cards.EventCards;

import it.polimi.ingws.gc8.Model.Enum.Era;
import it.polimi.ingws.gc8.Model.Enum.EventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SustenanceEventCardTest {
    @Test
    @DisplayName("Should correctly initialize SustenanceEventCard and verify all inherited fields")
    void TestSustenanceEventCarsInitialization(){
        String front = "sustenance_front.png";
        String back = "sustenance_back.png";
        Era era = Era.ERA_1;
        boolean isFinal = false;
        EventType eventType = EventType.sustenance;
        int prestigeMalus = 1;

        SustenanceEventCard sustenance = new SustenanceEventCard(front, back, era, isFinal,eventType, prestigeMalus);

        assertAll("Verify sustenance properties",
                () -> assertEquals(front, sustenance.getFront(), "Front image mismatch"),
                () -> assertEquals(back, sustenance.getBack(), "Back image mismatch"),
                () -> assertEquals(Era.ERA_1, sustenance.getEra(), "Era mismatch"),
                () -> assertEquals(isFinal, sustenance.isFinal(), "isFinal mismatch"),
                () -> assertEquals(eventType, sustenance.getEventType(), "eventType must be cavePaintings"),
                () -> assertEquals(prestigeMalus, sustenance.getPrestigeMalus(), "prestigeMalus mismatch")
        );
    }
}
