package it.polimi.ingws.gc8.Model.Cards.EventCards;

import it.polimi.ingws.gc8.Model.Enum.CharacterType;
import it.polimi.ingws.gc8.Model.Enum.Era;
import it.polimi.ingws.gc8.Model.Enum.EventType;
import it.polimi.ingws.gc8.Model.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CavePaintingsEventCard extends EventCard {

    private final int bonusArtists;
    private final int[] malusArtists;
    private final int bonusPoints;
    private final int malusPoints;

    public CavePaintingsEventCard(String front, String back, Era era, boolean isFinal, EventType eventType,
                                  int bonusArtists, int[] malusArtists, int bonusPoints, int malusPoints) {
        super(front, back, era, isFinal, eventType);
        this.bonusArtists = bonusArtists;
        this.malusArtists = malusArtists;
        this.bonusPoints = bonusPoints;
        this.malusPoints = malusPoints;
    }

    @Override
    public void resolveEvent(List<Player> players) {
        for(Player p : players) {
            if(p.getTribe().countByType(CharacterType.ARTIST) == bonusArtists) {
                p.addPrestigePoints(bonusPoints * p.getTribe().countByType(CharacterType.ARTIST));
            }
            if(Arrays.asList(malusArtists).contains(p.getTribe().countByType(CharacterType.ARTIST))) {
                p.removePrestigePoints(malusPoints);
            }
        }
    }

    public int getBonusArtists() {
        return bonusArtists;
    }

    public int[] getMalusArtists() {

        return malusArtists;
    }

    public int getBonusPoints() {

        return bonusPoints;
    }

    public int getMalusPoints() {

        return malusPoints;
    }
}
