package it.polimi.ingws.gc8.Model.Cards.CharacterCards;

import it.polimi.ingws.gc8.Model.Enum.CharacterType;
import it.polimi.ingws.gc8.Model.Enum.Era;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HunterTest {

    @Test
    @DisplayName("Should correctly initialize all fields for ERA_1 with bonus")
    void testHunterCreationEra1() {

        String front = "hunter_1_f.png";
        String back = "hunter_1_b.png";
        Era era = Era.ERA_1;
        int players = 2;
        boolean bonus = true;

        // Initializes card
        Hunter hunter = new Hunter(front, back, era, players, bonus);

        // Assert
        assertEquals(front, hunter.getFront(), "Front path should be correctly assigned.");
        assertEquals(back, hunter.getBack(), "Back path should be correctly assigned.");
        assertEquals(Era.ERA_1, hunter.getEra(), "The era should be ERA_1.");
        assertEquals(CharacterType.HUNTER, hunter.getCharacterType(), "The CharacterType must be fixed to HUNTER.");
        assertTrue(hunter.hasBonus(), "Bonus flag should be true.");

    }

    @Test
    @DisplayName("Should correctly initialize for ERA_3 without bonus")
    void testHunterCreationEra3() {

        // Initializes card
        Hunter hunter = new Hunter("f3", "b3", Era.ERA_3, 4, false);

        // Assert
        assertEquals(Era.ERA_3, hunter.getEra());
        assertFalse(hunter.hasBonus(), "Bonus flag should be false.");

    }

    @Test
    @DisplayName("Should verify player count boundary values")
    void testPlayerCount() {

        // Testing with a standard player count for ERA_2
        Hunter hunter = new Hunter("img", "img", Era.ERA_2, 3, true);

        assertEquals(3, hunter.getNumberOfPlayers(), "The player count should match the input value.");

    }

    @Test
    @DisplayName("Should ensure the character type is always HUNTER regardless of other params")
    void testTypeConsistency() {

        Hunter hunter1 = new Hunter("f", "b", Era.ERA_1, 2, true);
        Hunter hunter2 = new Hunter("f", "b", Era.ERA_2, 4, false);

        assertEquals(CharacterType.HUNTER, hunter1.getCharacterType());
        assertEquals(CharacterType.HUNTER, hunter2.getCharacterType());

    }
}