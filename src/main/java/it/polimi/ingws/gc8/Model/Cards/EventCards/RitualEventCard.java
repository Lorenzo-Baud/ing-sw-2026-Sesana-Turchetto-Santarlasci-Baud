package it.polimi.ingws.gc8.Model.Cards.EventCards;

import it.polimi.ingws.gc8.Model.Enum.Era;
import it.polimi.ingws.gc8.Model.Enum.EventType;
import it.polimi.ingws.gc8.Model.Player;

import java.util.List;

public class RitualEventCard extends EventCard {

    private final int prestigeMalus;
    private final int prestigeBonus;

    public RitualEventCard(String front, String back, Era era, boolean isFinal, EventType eventType,
                           int prestigeBonus, int prestigeMalus) {
        super(front, back, era, isFinal, eventType);
        this.prestigeMalus = prestigeMalus;
        this.prestigeBonus = prestigeBonus;
    }

    @Override
    public void resolveEvent(List<Player> players) {

    }

}
