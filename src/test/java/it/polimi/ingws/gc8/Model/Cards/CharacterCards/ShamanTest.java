package it.polimi.ingws.gc8.Model.Cards.CharacterCards;

import it.polimi.ingws.gc8.Model.Enum.CharacterType;
import it.polimi.ingws.gc8.Model.Enum.Era;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ShamanTest {

    @Test
    @DisplayName("Should correctly initialize Shaman with icon count")
    void testShamanInitialization() {

        String front = "shaman_front.png";
        String back = "shaman_back.png";
        Era era = Era.ERA_1;
        int iconCount = 3;
        int players = 4;

        // Initializes class
        Shaman shaman = new Shaman(front, back, era, iconCount, players);

        // Assert
        assertAll("Verify Shaman attributes",
                () -> assertEquals(iconCount, shaman.getnIcons(), "Icon count mismatch"),
                () -> assertEquals(CharacterType.SHAMAN, shaman.getCharacterType(), "CharacterType must be SHAMAN"),
                () -> assertEquals(front, shaman.getFront(), "Front image mismatch"),
                () -> assertEquals(back, shaman.getBack(), "Back image mismatch"),
                () -> assertEquals(Era.ERA_1, shaman.getEra(), "Era mismatch"),
                () -> assertEquals(players, shaman.getNumberOfPlayers(), "Player count mismatch")
        );

    }

    @Test
    @DisplayName("Should handle minimum icon values")
    void testShamanMinIcons() {

        // Testing with 1 icon (assuming it's the logical minimum for a Shaman)
        Shaman shaman = new Shaman("f", "b", Era.ERA_2, 1, 2);

        assertEquals(1, shaman.getnIcons(), "The icon count should be exactly 1.");

    }

    @Test
    @DisplayName("Should verify that multiple Shaman instances maintain their own state")
    void testShamanIndependence() {

        Shaman shaman1 = new Shaman("f1", "b1", Era.ERA_1, 2, 2);
        Shaman shaman2 = new Shaman("f2", "b2", Era.ERA_3, 5, 4);

        assertNotEquals(shaman1.getnIcons(), shaman2.getnIcons());
        assertNotEquals(shaman1.getEra(), shaman2.getEra());

    }
}