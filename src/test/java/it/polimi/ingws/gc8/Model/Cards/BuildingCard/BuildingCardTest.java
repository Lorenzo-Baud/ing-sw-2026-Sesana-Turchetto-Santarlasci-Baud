package it.polimi.ingws.gc8.Model.Cards.BuildingCard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import it.polimi.ingws.gc8.Model.Cards.CharacterCards.*;
import it.polimi.ingws.gc8.Model.Cards.EventCards.EventCard;
import it.polimi.ingws.gc8.Model.Cards.EventCards.SustenanceEventCard;
import it.polimi.ingws.gc8.Model.Enum.CardType;
import it.polimi.ingws.gc8.Model.Enum.Era;
import it.polimi.ingws.gc8.Model.Enum.EventType;
import it.polimi.ingws.gc8.Model.Enum.TotemColor;
import it.polimi.ingws.gc8.Model.Player;
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
        List<BuildingCard> cards = BuildingCard.loadFromJson("/buildings_cards.json");

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

    @Test
    void testInventorPairEffect() {
        Player player = new Player(1, "TestPlayer", TotemColor.RED);
        BuildingEffect effect = new InventorPairEffect(3); // Example: 3 food reward

        Inventor inventor1 = new Inventor("building_front.png", "building_back.png", Era.ERA_1, 2, 4);

        player.getTribe().addCharacter(player, inventor1);
        effect.onCardAdded(player, inventor1);
        assertEquals(0, player.getFood(), "The first inventor should grant no food");

        Inventor inventor2 = new Inventor("f2", "building_back.png", Era.ERA_2, 5, 6);
        player.getTribe().addCharacter(player, inventor2);
        effect.onCardAdded(player, inventor2);
        assertEquals(0, player.getFood(), "The second inventor with an icon different from the icon of the first inventor should grant no food");

        Inventor inventor3 = new Inventor("building_front.png", "building_back.png", Era.ERA_2, 5, 6);
        player.getTribe().addCharacter(player, inventor3);
        effect.onCardAdded(player, inventor2);
        assertEquals(3, player.getFood(), "The third inventor has the same icon of the second inventor so should grant 3 food points");

        Inventor inventor4 = new Inventor("building_front.png", "building_back.png", Era.ERA_1, 5, 4);
        player.getTribe().addCharacter(player, inventor4);
        effect.onCardAdded(player, inventor2);
        assertEquals(6, player.getFood(), "The fourth inventor has the same icon of the first inventor so should grant 3 food points");

    }

    @Test
    void testSetCompletionEffect() {
        Player player = new Player(1, "TestPlayer", TotemColor.RED);

        SetCompletionEffect setCompletionEffect = new SetCompletionEffect(3);

        player.getTribe().addCharacter(player, new Inventor("building_front.png", "building_back.png", Era.ERA_1, 2, 4));
        player.getTribe().addCharacter(player, new Shaman("shaman_front.png", "shaman_back.png", Era.ERA_1, 5, 6));
        player.getTribe().addCharacter(player, new Artist("artist_front.png", "artist_back.png", Era.ERA_1, 5));
        player.getTribe().addCharacter(player, new Builder("builder_front.png", "builder_back.png", Era.ERA_1,4,2, 5));
        Collector collector = new Collector("collector_front.png", "collector_back.png", Era.ERA_1,5,3);
        player.getTribe().addCharacter(player, collector);
        setCompletionEffect.onCardAdded(player,collector);

        assertEquals(0, player.getFood(), "5 characters of different type should grant no food points");
        Hunter hunter = new Hunter("hunter_front.png", "hunter_back.png", Era.ERA_1, 5, false);
        player.getTribe().addCharacter(player, hunter);
        setCompletionEffect.onCardAdded(player,hunter);

        assertEquals(3, player.getFood(), "6 characters of different type should grant 3 food points");

    }

    @Test
    public void testBuildingGetters() {

        Player player = new Player(1, "TestPlayer", TotemColor.RED);
        SetCompletionEffect setCompletionEffect = new SetCompletionEffect(3);

        assertEquals(0, setCompletionEffect.getSustenanceFoodReduction(player));
        assertEquals(0, setCompletionEffect.getExtraFoodOnTurnOrder());
        assertEquals(0, setCompletionEffect.getExtraFoodOnTurnOrder());
    }

    @Test
    public void testBuildingEffectCreationException() {
        assertThrows(IllegalArgumentException.class, () -> {
            BuildingEffect be = BuildingEffect.createFrom(
                    "SET", null
            );
        });

    }

    @Test
    public void testBuildingEffects() {

        Player player = new Player(1, "TestPlayer", TotemColor.RED);
        EventCard eventCard = new SustenanceEventCard(
                "", "", Era.ERA_1, false, EventType.sustenance, 0
        );

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode params = mapper.createObjectNode();

        params.put("sustenanceType", "SHAMAN");
        params.put("sustenanceReductionPerUnit", 1);
        params.put("shield", true);
        params.put("endGameStrategy", "OTHER");

        ConfigurableEffect effect = ConfigurableEffect.fromJson(params);

        effect.onEvent(player, eventCard);
        assertEquals(0, effect.onGameEnd(player));

        assertEquals(0, effect.getShamanIconBonus());
        assertTrue(effect.preventsShamanPenalty());
        assertFalse(effect.doublesShamanBonus());
        assertFalse(effect.gainsOnShamanTie());
        assertEquals(0, effect.getSustenanceFoodReduction(player));
        assertFalse(effect.canPickExtraCard());
        assertEquals(0, effect.getExtraFoodOnTurnOrder());
    }

    @Test
    public void testOnGameEnd() {

        Player player = new Player(1, "TestPlayer", TotemColor.RED);
        EventCard eventCard = new SustenanceEventCard(
                "", "", Era.ERA_1, false, EventType.sustenance, 0
        );
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode params = mapper.createObjectNode();
        ConfigurableEffect effect = ConfigurableEffect.fromJson(params);

        assertEquals(0, effect.onGameEnd(player));

        params.put("endGameStrategy", "TYPE");
        effect = ConfigurableEffect.fromJson(params);
        assertEquals(0, effect.onGameEnd(player));

        params.put("endGameStrategy", "SET");
        effect = ConfigurableEffect.fromJson(params);
        assertEquals(0, effect.onGameEnd(player));

        params.put("endGameStrategy", "DOUBLE_BUILDER");
        effect = ConfigurableEffect.fromJson(params);
        assertEquals(0, effect.onGameEnd(player));


    }

}