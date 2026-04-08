package it.polimi.ingws.gc8.Model.Cards.EventCards;

import it.polimi.ingws.gc8.Model.Cards.CharacterCard;
import it.polimi.ingws.gc8.Model.Enum.CharacterType;
import it.polimi.ingws.gc8.Model.Enum.Era;
import it.polimi.ingws.gc8.Model.Enum.EventType;
import it.polimi.ingws.gc8.Model.Player;
import it.polimi.ingws.gc8.Model.Tribe;

import java.util.List;

public class SustenanceEventCard extends EventCard {

    private final int prestigeMalus;

    public SustenanceEventCard(String front, String back, Era era, boolean isFinal, EventType eventType,
                               int prestigeMalus) {
        super(front, back, era, isFinal, eventType);
        this.prestigeMalus = prestigeMalus;
    }

    @Override
    public void resolveEvent(List<Player> players) {
        for (Player player : players) {
            Tribe tribe = player.getTribe();

            int totalCharacters = tribe.getTotalCharacters();
            int collectorDiscount = tribe.getCollectorDiscount();
            int buildingDiscount = tribe.getTotalSustenanceReduction(player);
            int foodToPay = Math.max(0, totalCharacters - collectorDiscount - buildingDiscount);

            if (foodToPay <= player.getFood()) {
                player.removeFood(foodToPay);
            } else {
                int unfed = foodToPay - player.getFood();
                player.removeFood(player.getFood());
                player.removePrestigePoints(unfed * prestigeMalus);
            }
        }
    }

    public int getPrestigeMalus() {
        return prestigeMalus;
    }
}
