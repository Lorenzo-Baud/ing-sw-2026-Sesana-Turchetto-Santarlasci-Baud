package it.polimi.ingws.gc8.Model.Cards.CharacterCards;

import it.polimi.ingws.gc8.Model.Enum.CharacterType;
import it.polimi.ingws.gc8.Model.Enum.Era;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BuilderTest {

    @Test
    @DisplayName("Should correctly initialize Builder with specific bonuses and points")
    void testBuilderInitialization() {

        String front = "builder_f.png";
        String back = "builder_b.png";
        Era era = Era.ERA_2;
        int food = 3;
        int prestige = 5;
        int players = 2;

        // Initializes class
        Builder builder = new Builder(front, back, era, food, prestige, players);

        // Assert
        assertAll("Verify Builder attributes",
                () -> assertEquals(food, builder.getFoodBonus(), "Food bonus mismatch"),
                () -> assertEquals(prestige, builder.getPrestigePoints(), "Prestige points mismatch"),
                () -> assertEquals(CharacterType.BUILDER, builder.getCharacterType(), "CharacterType must be BUILDER"),
                () -> assertEquals(Era.ERA_2, builder.getEra(), "Era mismatch"),
                () -> assertEquals(players, builder.getNumberOfPlayers(), "Player count mismatch")
        );

    }

    @Test
    @DisplayName("Should handle zero or negative values if permitted by logic")
    void testBuilderBoundaryValues() {

        // Testing if the object can hold 0 values (common in game cards)
        Builder builder = new Builder("f", "b", Era.ERA_1, 0, 0, 4);

        assertEquals(0, builder.getFoodBonus());
        assertEquals(0, builder.getPrestigePoints());

    }

    @Test
    @DisplayName("Should verify field consistency")
    void testGetterConsistency() {

        // Initializes class
        Builder builder = new Builder("f", "b", Era.ERA_3, 1, 10, 3);

        // Ensure values don't change between calls
        int firstFoodCall = builder.getFoodBonus();
        int firstPrestigeCall = builder.getPrestigePoints();

        assertEquals(firstFoodCall, builder.getFoodBonus());
        assertEquals(firstPrestigeCall, builder.getPrestigePoints());

    }
}