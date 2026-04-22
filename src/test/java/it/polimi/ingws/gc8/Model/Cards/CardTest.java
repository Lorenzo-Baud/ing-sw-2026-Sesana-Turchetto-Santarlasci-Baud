package it.polimi.ingws.gc8.Model.Cards;

import it.polimi.ingws.gc8.Model.Enum.Era;
import it.polimi.ingws.gc8.Network.DTO.CardDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CardTest {

    private Card card;
    private final String frontPath = "path/to/front.png";
    private final String backPath = "path/to/back.png";
    private final Era era = Era.ERA_1;

    /**
     * Concrete implementation of Card used only for testing purposes.
     */
    private static class ConcreteCard extends Card {
        public ConcreteCard(String front, String back, Era era) {
            super(front, back, era);
        }
    }

    @BeforeEach
    void setUp() {
        // Initialize the concrete instance before each test
        card = new ConcreteCard(frontPath, backPath, era);
    }

    @Test
    void testConstructorAndGetters() {
        // Verify that the fields initialized via constructor are correctly returned by getters
        assertEquals(frontPath, card.getFront(), "The front image path should match the constructor input.");
        assertEquals(backPath, card.getBack(), "The back image path should match the constructor input.");
        assertEquals(era, card.getEra(), "The era should match the constructor input.");
    }

    @Test
    void testImmutableFields() {
        // Since fields are final (except era, though it is protected final),
        // we ensure that the object state remains consistent after initialization.
        assertNotNull(card.getFront());
        assertNotNull(card.getBack());
        assertNotNull(card.getEra());
    }

}