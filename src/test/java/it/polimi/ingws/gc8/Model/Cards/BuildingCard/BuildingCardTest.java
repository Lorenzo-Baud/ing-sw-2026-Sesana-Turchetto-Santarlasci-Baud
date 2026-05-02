package it.polimi.ingws.gc8.Model.Cards.BuildingCard;

import it.polimi.ingws.gc8.Model.Enum.CardType;
import it.polimi.ingws.gc8.Model.Enum.Era;
import it.polimi.ingws.gc8.Network.DTO.CardDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


class BuildingCardTest {

    @TempDir
    Path tempDir;

    @Test
    void testGettersAndConstructor() {
        BuildingCard card = new BuildingCard("B01", 5, 10, Era.ERA_1, "f.png", "b.png", null);

        assertEquals("B01", card.getId());
        assertEquals(5, card.getFoodCost());
        assertEquals(10, card.getPrestigeBonus());
        assertNull(card.getEffect());
        assertEquals(Era.ERA_1, card.getEra());
    }

    @Test
    void testGetEffectiveCost() {
        BuildingCard card = new BuildingCard("B01", 10, 5, Era.ERA_1, "f", "b", null);

        assertEquals(7, card.getEffectiveCost(3));
        assertEquals(0, card.getEffectiveCost(15));
        assertEquals(10, card.getEffectiveCost(0));
    }

    @Test
    void testLoadFromJson_RealFile() {
        List<BuildingCard> cards = BuildingCard.loadFromJson("src/main/resources/buildings_cards.json");

        assertNotNull(cards);
        assertFalse(cards.isEmpty(), "The list should not be empty if the JSON is correctly populated");

        BuildingCard first = cards.get(0);
        assertNotNull(first.getId());
        assertNotNull(first.getEra());
    }

    @Test
    void testLoadFromJson_FileNotFound() {
        List<BuildingCard> cards = BuildingCard.loadFromJson("wrong/path/to/file.json");

        assertNotNull(cards);
        assertTrue(cards.isEmpty());
    }

    @Test
    void testLoadFromJson_Exception() {
        List<BuildingCard> cards = BuildingCard.loadFromJson("non_existent_file.json");
        assertTrue(cards.isEmpty());
    }

}