package it.polimi.ingws.gc8.Controller;

import it.polimi.ingws.gc8.Network.DTO.MatchInfo;
import it.polimi.ingws.gc8.Network.DTO.MatchStatus;
import it.polimi.ingws.gc8.Persistence.PersistenceManager;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MatchRegistry {

    /** Maximum length of a user-chosen match name. */
    public static final int MAX_MATCH_NAME_LEN = 24;

    private final Map<String, MainController> matches = new ConcurrentHashMap<>();

    /** Persistence: shared persistence manager; null disables persistence. */
    private final PersistenceManager pm;

    public MatchRegistry() {
        this(null);
    }

    public MatchRegistry(PersistenceManager pm) {
        this.pm = pm;
    }

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
        synchronized (matches) {
            if (matches.containsKey(id)) {
                throw new IllegalStateException("Esiste già una partita con questo nome: " + id);
            }
            MainController controller = new MainController(id, this, pm);
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

    /**
     * FA2: re-registers a match rebuilt from a snapshot at server startup
     * (status IN_GAME + suspended, awaiting reconnections by nickname).
     *
     * @param matchId    the restored match id
     * @param controller the rebuilt controller
     */
    public void reattach(String matchId, MainController controller) {
        matches.put(matchId, controller);
    }

    /**
     * FA2: ordered shutdown of all matches so pending snapshot writes are
     * flushed before the JVM exits. Invoked from the server shutdown hook.
     */
    public void shutdownAll() {
        for (MainController m : matches.values()) {
            m.flush();
        }
    }

    public List<MatchInfo> listOpenMatches() {
        return matches.values().stream()
                .filter(m -> m.getStatus() == MatchStatus.LOBBY)
                .map(MainController::toMatchInfo)
                .toList();
    }
}
