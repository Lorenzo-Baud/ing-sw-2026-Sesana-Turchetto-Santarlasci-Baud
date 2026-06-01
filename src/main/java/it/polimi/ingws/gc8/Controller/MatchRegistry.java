package it.polimi.ingws.gc8.Controller;

import it.polimi.ingws.gc8.Network.DTO.MatchInfo;
import it.polimi.ingws.gc8.Network.DTO.MatchStatus;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MatchRegistry {

    private final Map<String, MainController> matches = new ConcurrentHashMap<>();

    public MainController createMatch() {
        String matchId = UUID.randomUUID().toString().substring(0, 8);
        MainController controller = new MainController(matchId, this);
        matches.put(matchId, controller);
        return controller;
    }

    public MainController getMatch(String matchId) {
        return matches.get(matchId);
    }

    public void removeMatch(String matchId) {
        matches.remove(matchId);
    }

    public List<MatchInfo> listOpenMatches() {
        return matches.values().stream()
                .filter(m -> m.getStatus() == MatchStatus.LOBBY)
                .map(MainController::toMatchInfo)
                .toList();
    }
}
