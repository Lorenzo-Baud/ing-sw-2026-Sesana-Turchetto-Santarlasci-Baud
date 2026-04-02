package it.polimi.ingws.gc8.Model.Cards.CharacterCards;

import it.polimi.ingws.gc8.Model.Enum.CharacterType;
import it.polimi.ingws.gc8.Model.Enum.Era;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InventorTest {

    private final String front = "inventor_front.png";
    private final String back = "inventor_back.png";
    private final Era era = Era.ERA_2;
    private final int players = 3;
    private final int iconId = 4;

    private Inventor inventor;

    @BeforeEach
    void setUp() {
        // Initialize a new Inventor instance before each test to ensure isolation
        inventor = new Inventor(front, back, era, players, iconId);
    }

    @Test
    @DisplayName("Should correctly initialize Inventor specific fields")
    void testInventorInitialization() {
        // Verify the specific attribute for this subclass
        assertEquals(iconId, inventor.getIcon(), "The icon ID should match the constructor input.");
    }

    @Test
    @DisplayName("Should verify inherited attributes from Card and CharacterCard")
    void testInheritedAttributes() {
        // Verify attributes defined in the parent classes
        assertEquals(front, inventor.getFront(), "Front path mismatch.");
        assertEquals(back, inventor.getBack(), "Back path mismatch.");
        assertEquals(Era.ERA_2, inventor.getEra(), "Era mismatch.");
        assertEquals(players, inventor.getNumberOfPlayers(), "Player count mismatch.");
        assertEquals(CharacterType.INVENTOR, inventor.getCharacterType(), "Type must be set to INVENTOR.");
    }

    @Test
    @DisplayName("Should handle different icon values correctly")
    void testDifferentIconValues() {
        // Test with a different icon value (e.g., 0 or negative if allowed by logic)
        Inventor specificInventor = new Inventor("f", "b", Era.ERA_3, 4, 0);
        assertEquals(0, specificInventor.getIcon(), "The icon value 0 should be handled correctly.");
    }

}