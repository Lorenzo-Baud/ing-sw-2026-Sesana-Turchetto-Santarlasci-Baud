package it.polimi.ingws.gc8.Model.Cards.EventCards;

import it.polimi.ingws.gc8.Model.Enum.Era;
import it.polimi.ingws.gc8.Model.Enum.EventType;
import it.polimi.ingws.gc8.Model.Player;

import java.util.List;

public class HuntEventCard extends EventCard {

    private final int prestigeBonus;

    public HuntEventCard(String front, String back, Era era, boolean isFinal, EventType eventType, int prestigeBonus) {
        super(front, back, era, isFinal, eventType);
        this.prestigeBonus = prestigeBonus;
    }

    @Override
    public void resolveEvent(List<Player> players) {

    }

}
