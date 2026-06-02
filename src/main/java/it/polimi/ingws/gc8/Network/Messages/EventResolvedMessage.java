package it.polimi.ingws.gc8.Network.Messages;

import java.io.Serializable;
import java.util.List;

/**
 * Notifies clients that one or more Event cards have just been resolved
 * (at the end of a round / era). Carries only structured, view-agnostic data:
 * each view (TUI/GUI) decides how to render it. No presentation strings are
 * built server-side.
 *
 * <p>For every resolved event it reports the event type, whether it was a
 * final event, and the per-player prestige variation it produced (only
 * players whose prestige actually changed are listed).</p>
 */
public class EventResolvedMessage implements NetworkMessage {
    private static final long serialVersionUID = 1L;

    /** Prestige variation a single player underwent because of one event. */
    public record PlayerDelta(String nickname, int prestigeDelta) implements Serializable {
        private static final long serialVersionUID = 1L;
    }

    /** Outcome of resolving a single Event card. */
    public record ResolvedEvent(String eventType, boolean isFinal,
                                List<PlayerDelta> deltas) implements Serializable {
        private static final long serialVersionUID = 1L;
    }

    private final List<ResolvedEvent> events;

    public EventResolvedMessage(List<ResolvedEvent> events) {
        this.events = events;
    }

    public List<ResolvedEvent> getEvents() {
        return events;
    }
}
