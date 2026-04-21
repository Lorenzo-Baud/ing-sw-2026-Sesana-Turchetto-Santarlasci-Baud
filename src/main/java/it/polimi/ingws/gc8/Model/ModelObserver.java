package it.polimi.ingws.gc8.Model;

/**
 * Observer notified whenever the {@link GameModel} state changes.
 * Implementations live outside the Model layer (e.g., in Network or View)
 * and must not mutate the snapshot they receive.
 */
public interface ModelObserver {

    /**
     * Invoked by the model after a state mutation.
     *
     * @param snapshot the current model, exposed through its read-only interface
     */
    void onModelChanged(GameModel snapshot);
}
