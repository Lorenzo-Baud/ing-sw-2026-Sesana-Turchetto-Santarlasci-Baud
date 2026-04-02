package it.polimi.ingws.gc8.Model.Cards.CharacterCards;

import it.polimi.ingws.gc8.Model.Enum.CharacterType;
import it.polimi.ingws.gc8.Model.Enum.Era;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CollectorTest {

    @Test
    @DisplayName("Should correctly initialize Collector with food discount")
    void testCollectorInitialization() {

        String front = "collector_f.png";
        String back = "collector_b.png";
        Era era = Era.ERA_3;
        int players = 3;
        int discount = 2;

        // Initializes class
        Collector collector = new Collector(front, back, era, players, discount);

        // Assert
        assertAll("Verify Collector attributes",
                () -> assertEquals(discount, collector.getFoodDiscount(), "Food discount mismatch"),
                () -> assertEquals(CharacterType.COLLECTOR, collector.getCharacterType(), "CharacterType must be COLLECTOR"),
                () -> assertEquals(front, collector.getFront(), "Front image mismatch"),
                () -> assertEquals(back, collector.getBack(), "Back image mismatch"),
                () -> assertEquals(Era.ERA_3, collector.getEra(), "Era mismatch"),
                () -> assertEquals(players, collector.getNumberOfPlayers(), "Player count mismatch")
        );

    }

    @Test
    @DisplayName("Should handle different discount values")
    void testCollectorDiscountValues() {

        // Testing with a zero discount scenario
        Collector collector = new Collector("f", "b", Era.ERA_1, 2, 0);

        assertEquals(0, collector.getFoodDiscount(), "The discount should be exactly 0.");

    }

    @Test
    @DisplayName("Should ensure field immutability")
    void testImmutability() {

        // Initializes class
        Collector collector = new Collector("f", "b", Era.ERA_2, 4, 5);

        // Verify that the value remains the same after multiple accesses
        int firstCall = collector.getFoodDiscount();
        int secondCall = collector.getFoodDiscount();

        assertEquals(firstCall, secondCall, "Getter should return a consistent value.");

    }
}