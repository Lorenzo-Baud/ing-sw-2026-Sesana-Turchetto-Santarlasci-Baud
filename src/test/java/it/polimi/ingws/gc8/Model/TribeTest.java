package it.polimi.ingws.gc8.Model;

import it.polimi.ingws.gc8.Model.Cards.BuildingCard.BuildingCard;
import it.polimi.ingws.gc8.Model.Cards.BuildingCard.ConfigurableEffect;
import it.polimi.ingws.gc8.Model.Cards.BuildingCard.SetCompletionEffect;
import it.polimi.ingws.gc8.Model.Cards.CharacterCards.Artist;
import it.polimi.ingws.gc8.Model.Cards.CharacterCards.Builder;
import it.polimi.ingws.gc8.Model.Cards.CharacterCards.Collector;
import it.polimi.ingws.gc8.Model.Cards.CharacterCards.Shaman;
import it.polimi.ingws.gc8.Model.Cards.EventCards.EventCard;
import it.polimi.ingws.gc8.Model.Cards.EventCards.SustenanceEventCard;
import it.polimi.ingws.gc8.Model.Enum.CharacterType;
import it.polimi.ingws.gc8.Model.Enum.Era;
import it.polimi.ingws.gc8.Model.Enum.EventType;
import it.polimi.ingws.gc8.Model.Enum.TotemColor;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/*
    Class to test the various methods of tribe test
    that have not been tested in other tests. Increasing coverage
 */
public class TribeTest {

    Tribe tribe = new Tribe();

    @Test
    public void testGetTotalBuilderDiscount() {
        tribe.addCharacter(
                new Player(0, "", TotemColor.RED),
                new Builder("", "", Era.ERA_1, 3, 0, 1)
        );

        assertEquals(3, tribe.getTotalBuilderDiscount());

    }

    @Test
    public void testGetCollectorDiscount() {
        tribe.addCharacter(
                new Player(0, "", TotemColor.RED),
                new Collector("", "", Era.ERA_1, 3, 3)
        );

        assertEquals(3, tribe.getCollectorDiscount());

    }

    @Test
    public void testGetDistinctCharacterTypesCount() {

        tribe.addCharacter(
                new Player(0, "", TotemColor.RED),
                new Builder("", "", Era.ERA_1, 3, 0, 1)
        );

        tribe.addCharacter(
                new Player(0, "", TotemColor.RED),
                new Collector("", "", Era.ERA_1, 3, 3)
        );

        assertEquals(2, tribe.getDistinctCharacterTypesCount());

    }

    @Test
    public void testGetPrintedBuildingPrestige() {

        tribe.addBuilding(
                new BuildingCard("", 1, 1, Era.ERA_1, "", "",
                        new SetCompletionEffect(2))
        );
        tribe.addBuilding(
                new BuildingCard("", 1, 1, Era.ERA_1, "", "",
                        new SetCompletionEffect(2))
        );

        tribe.triggerOnEvent(
                new Player(0, "", TotemColor.RED),
                new SustenanceEventCard(
                        "", "", Era.ERA_1, false,
                        EventType.sustenance, 2)
        );

        tribe.triggerOnTurnEnd(
                new Player(0, "", TotemColor.RED),
                0
        );

        tribe.triggerOnGameEnd(
                new Player(0, "", TotemColor.RED)
        );

        assertEquals(2, tribe.getPrintedBuildingPrestige());

    }

    @Test
    public void testShamanRelatedMethods() {

        tribe.addCharacter(
                new Player(0, "", TotemColor.RED),
                new Shaman("", "", Era.ERA_1, 3,  3)
        );

        tribe.addCharacter(
                new Player(0, "", TotemColor.RED),
                new Shaman("", "", Era.ERA_1, 1,  3)
        );

        tribe.addBuilding(
                new BuildingCard("", 1, 1, Era.ERA_1, "", "",
                        new SetCompletionEffect(2))
        );

        assertEquals(0, tribe.getTotalShamanIconBonus());
        assertFalse(tribe.hasShamanShield());
        assertFalse(tribe.hasShamanDouble());
        assertFalse(tribe.hasShamanGainOnTie());
        assertFalse(tribe.canPickExtraCard());

    }

    // Test to increase branch coverage
    @Test
    public void testBranchCoverage() {

        Tribe tribe = new Tribe();
        tribe.addCharacter(
                new Player(1, "", TotemColor.RED),
                new Artist("f", "b", Era.ERA_1, 2));

        assertEquals(1, tribe.countByType(Artist.class));
        assertEquals(0, tribe.getTotalShamanIcons());

    }

}
