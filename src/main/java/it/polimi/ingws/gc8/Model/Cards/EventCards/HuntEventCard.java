package it.polimi.ingws.gc8.Model.Cards.EventCards;

import it.polimi.ingws.gc8.Model.Enum.CharacterType;
import it.polimi.ingws.gc8.Model.Enum.Era;
import it.polimi.ingws.gc8.Model.Enum.EventType;
import it.polimi.ingws.gc8.Model.Player;
import it.polimi.ingws.gc8.Model.Tribe;

import java.util.List;

public class HuntEventCard extends EventCard {

    private final int prestigeBonus;

    public HuntEventCard(String front, String back, Era era, boolean isFinal, EventType eventType, int prestigeBonus) {
        super(front, back, era, isFinal, eventType);
        this.prestigeBonus = prestigeBonus;
    }

    @Override
    public void resolveEvent(List<Player> players) {
        for (Player player : players) {
            Tribe tribe = player.getTribe();
            int hunters = tribe.countByType(CharacterType.HUNTER);

            // Base reward: +1 food and +prestigeBonus PP per Hunter
            player.addFood(hunters);
            player.addPrestigePoints(hunters * prestigeBonus);

            // Building effects (e.g. E2_06: extra food and PP per Hunter)
            tribe.triggerOnEvent(player, this);
        }
    }

    public int getPrestigeBonus() {
        return prestigeBonus;
    }
}
