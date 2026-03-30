package it.polimi.ingws.gc8.Model.Cards.EventCards;

import it.polimi.ingws.gc8.Model.Enum.Era;
import it.polimi.ingws.gc8.Model.Enum.EventType;
import it.polimi.ingws.gc8.Model.Player;

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

    }

    public int getPrestigeMalus() {
        return prestigeMalus;
    }
}
