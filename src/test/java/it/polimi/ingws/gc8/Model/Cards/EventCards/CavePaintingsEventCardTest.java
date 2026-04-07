package it.polimi.ingws.gc8.Model.Cards.EventCards;

import it.polimi.ingws.gc8.Model.Enum.CharacterType;
import it.polimi.ingws.gc8.Model.Enum.Era;
import it.polimi.ingws.gc8.Model.Enum.EventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CavePaintingsEventCardTest {
    @Test
    @DisplayName("Should correctly initialize CavePaintingEventCard and verify all inherited fields")
    void testCavePaintingInitialization(){
        String front = "cave_paintings_front.png";
        String back = "cave_paintings_back.png";
        Era era = Era.ERA_1;
        boolean isFinal = false;
        EventType eventType = EventType.cavePaintings;
        int bonusArtists = 1;
        int malusArtists = 0;
        int bonusPoints = 1;
        int malusPoints = 2;

        CavePaintingsEventCard cavePainting = new CavePaintingsEventCard(front, back, era, isFinal, eventType, bonusArtists, malusArtists, bonusPoints, malusPoints);

        assertAll("Verify CavePaintings properties",
                () -> assertEquals(front, cavePainting.getFront(), "Front image mismatch"),
                () -> assertEquals(back, cavePainting.getBack(), "Back image mismatch"),
                () -> assertEquals(Era.ERA_1, cavePainting.getEra(), "Era mismatch"),
                () -> assertEquals(isFinal, cavePainting.isFinal(), "isFinal mismatch"),
                () -> assertEquals(EventType.cavePaintings, cavePainting.getEventType(), "eventType must be cavePaintings"),
                () -> assertEquals(bonusArtists, cavePainting.getBonusArtists(), "bonusArtists mismatch"),
                () -> assertEquals(malusArtists, cavePainting.getMalusArtists(), "malusArtists mismatch"),
                () -> assertEquals(bonusPoints, cavePainting.getBonusPoints(), "bonusPoints mismatch"),
                () -> assertEquals(malusPoints, cavePainting.getMalusPoints())
        );
    }
}
