package it.polimi.ingws.gc8.Controller;

import it.polimi.ingws.gc8.Network.DTO.MatchInfo;
import it.polimi.ingws.gc8.Network.DTO.MatchStatus;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MatchRegistry {

    /** Maximum length of a user-chosen match name. */
    public static final int MAX_MATCH_NAME_LEN = 24;

    private final Map<String, MainController> matches = new ConcurrentHashMap<>();

    /**
     * Creates a new match using the host-chosen {@code matchName} as its id.
     *
     * @param matchName the desired match name; becomes the match id
     * @return the new {@link MainController}
     * @throws IllegalStateException if the name is blank, too long, or already in use
     */
    public MainController createMatch(String matchName) {
        if (matchName == null || matchName.isBlank()) {
            throw new IllegalStateException("Il nome della partita non può essere vuoto");
        }
        String id = matchName.trim();
        if (id.length() > MAX_MATCH_NAME_LEN) {
            throw new IllegalStateException(
                    "Nome partita troppo lungo (max " + MAX_MATCH_NAME_LEN + " caratteri)");
        }
        // Compound check-then-create must be atomic against concurrent creators.
        synchronized (matches) {
            if (matches.containsKey(id)) {
                throw new IllegalStateException("Esiste già una partita con questo nome: " + id);
            }
            MainController controller = new MainController(id, this);
            matches.put(id, controller);
            return controller;
        }
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
