package it.polimi.ingws.gc8.Persistence;

import it.polimi.ingws.gc8.Model.GameModel;

import java.io.Serializable;
import java.util.Map;

/**
 * FA2 — single serialization root for a persisted match.
 *
 * <p>Carries everything needed to rebuild a {@code MainController} after a
 * server crash: the full game {@link GameModel} plus the match metadata that
 * lives outside the model (ids, host, expected size, the per-match
 * nickname → playerId mapping used to reattach reconnecting clients).</p>
 *
 * <p>Deliberately excluded: connections, {@code ClientHandler}s,
 * {@code ViewUpdater}, threads, sockets. Those are "live" and are rebuilt on
 * reconnection, never written to disk.</p>
 */
public record MatchSnapshot(
        String matchId,
        int expectedPlayers,
        String hostNickname,
        int hostPlayerId,
        int nextPlayerId,
        Map<String, Integer> nickToPlayerId,
        GameModel model,
        long savedAtMillis
) implements Serializable {

    private static final long serialVersionUID = 1L;
}
