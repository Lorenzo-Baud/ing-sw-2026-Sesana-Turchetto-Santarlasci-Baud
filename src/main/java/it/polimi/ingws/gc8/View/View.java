package it.polimi.ingws.gc8.View;

import it.polimi.ingws.gc8.Client.ViewListener;
import it.polimi.ingws.gc8.Network.DTO.GameDTO;
import it.polimi.ingws.gc8.Network.Messages.NetworkMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Classe base per ogni View (TUI / futura GUI).
 *
 * <h2>Responsabilità</h2>
 * <ul>
 *   <li>Mantiene l'elenco dei {@link ViewListener} (il controller di rete).</li>
 *   <li>Mantiene una coda bloccante di {@link NetworkMessage} in ingresso:
 *       il thread di rete vi inserisce i messaggi, il thread UI li consuma.</li>
 *   <li>Mantiene lo stato "morbido" della UI: {@code nickname} scelto
 *       dall'utente e ultimo snapshot {@code gameView} ricevuto dal server.</li>
 * </ul>
 *
 * <h2>Threading</h2>
 * La coda {@code messages} è una {@code LinkedBlockingQueue} (thread-safe).
 * {@code gameView} e {@code nickname} sono {@code volatile}: scritti da un
 * thread (update dispatcher / main loop), letti da un altro (stampa).
 *
 * <h2>Lifecycle</h2>
 * La sottoclassa implementa {@link #run()} con la logica procedurale
 * della propria UI (per la TUI: menu iniziale, fasi di gioco, ecc.).
 */
public abstract class View {

    /** Listener registrati (tipicamente un solo ClientController). */
    protected final List<ViewListener> listeners = new ArrayList<>();

    /** Coda di messaggi provenienti dal server, consumata dal thread UI. */
    protected final LinkedBlockingQueue<NetworkMessage> messages = new LinkedBlockingQueue<>();

    /** Nickname scelto dall'utente, impostato in fase di login. */
    protected volatile String nickname;

    /** Ultimo snapshot autoritativo del gioco ricevuto dal server. */
    protected volatile GameDTO gameView;

    // ------------------------------------------------------------------
    // Listener registry
    // ------------------------------------------------------------------

    public void addListener(ViewListener listener) {
        if (listener != null) listeners.add(listener);
    }

    public void removeListener(ViewListener listener) {
        listeners.remove(listener);
    }

    /**
     * Notifica tutti i listener che l'utente ha prodotto un input/azione.
     * Il {@code message} è tipicamente un {@code PlayerAction}.
     */
    protected void notifyAllListeners(Object message) {
        for (ViewListener l : listeners) {
            l.updateFromView(message);
        }
    }

    // ------------------------------------------------------------------
    // Message queue: server -> view
    // ------------------------------------------------------------------

    /** Inserisce in coda un messaggio server. Chiamato dal pump di rete. */
    public void addMessage(NetworkMessage msg) {
        if (msg != null) messages.offer(msg);
    }

    /**
     * Attende (bloccante) il prossimo messaggio dal server.
     * Interrompibile: se il thread viene interrotto restituisce {@code null}.
     */
    protected NetworkMessage waitForMessage() {
        try {
            return messages.take();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    // ------------------------------------------------------------------
    // Accesso allo stato "morbido"
    // ------------------------------------------------------------------

    public String getNickname()          { return nickname; }
    public void   setNickname(String n)  { this.nickname = n; }
    public GameDTO getGameView()         { return gameView; }
    public void    setGameView(GameDTO v){ this.gameView = v; }

    // ------------------------------------------------------------------
    // Entry point
    // ------------------------------------------------------------------

    /** Ciclo di vita della UI: bloccante, chiamato dal main del client. */
    public abstract void run();
}
