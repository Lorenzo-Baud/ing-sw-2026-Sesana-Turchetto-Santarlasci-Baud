package it.polimi.ingws.gc8.Model.Cards.EventCards;

import it.polimi.ingws.gc8.Model.Cards.CharacterCard;
import it.polimi.ingws.gc8.Model.Cards.CharacterCards.Shaman;
import it.polimi.ingws.gc8.Model.Enum.CharacterType;
import it.polimi.ingws.gc8.Model.Enum.Era;
import it.polimi.ingws.gc8.Model.Enum.EventType;
import it.polimi.ingws.gc8.Model.Player;

import java.util.ArrayList;
import java.util.HashMap;
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
        ArrayList<Player> topPlayers = new ArrayList<>();
        ArrayList<Player> bottomPlayers = new ArrayList<>();

        int max = players.get(0).getTribe().getTotalShamanIcons();
        int min = players.get(0).getTribe().getTotalShamanIcons();

        for(Player p : players) {
            if(p.getTribe().getTotalShamanIcons() > max) {
                max = p.getTribe().getTotalShamanIcons();
            }

            if(p.getTribe().getTotalShamanIcons() < min) {
                min = p.getTribe().getTotalShamanIcons();
            }
        }

        for(Player p : players) {
            if(p.getTribe().getTotalShamanIcons() == max) {
                topPlayers.add(p);
            }

            if(p.getTribe().getTotalShamanIcons() == min) {
                bottomPlayers.add(p);
            }
        }

        for(Player p : topPlayers) {
            p.addPrestigePoints(prestigeBonus);
        }

        for(Player p : bottomPlayers) {
            p.removePrestigePoints(prestigeMalus);
        }
    }

    public int getPrestigeBonus() {
        return prestigeBonus;
    }

    public int getPrestigeMalus() {
        return prestigeMalus;
    }
}
