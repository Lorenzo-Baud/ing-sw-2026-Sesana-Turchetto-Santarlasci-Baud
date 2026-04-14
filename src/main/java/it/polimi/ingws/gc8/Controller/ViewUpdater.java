package it.polimi.ingws.gc8.Controller;

import it.polimi.ingws.gc8.Controller.events.GameEventListener;
import it.polimi.ingws.gc8.Model.Board.OfferTile;
import it.polimi.ingws.gc8.Model.Cards.Card;
import it.polimi.ingws.gc8.Model.Enum.EventType;
import it.polimi.ingws.gc8.Model.GameModel;
import it.polimi.ingws.gc8.Model.Player;
import it.polimi.ingws.gc8.Model.Enum.GamePhase;
import it.polimi.ingws.gc8.Network.DTO.CardDTO;
import it.polimi.ingws.gc8.Network.DTO.GameDTO;
import it.polimi.ingws.gc8.Network.DTO.OfferTileDTO;
import it.polimi.ingws.gc8.Network.DTO.PlayerDTO;
import it.polimi.ingws.gc8.Network.Messages.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Bridges the controller/model layer to the view layer.
 * Maintains a list of {@link GameEventListener} instances and broadcasts
 * game notifications to all of them.
 * <p>
 * All notify methods build immutable data from the model and broadcast it.
 * ViewUpdater never holds a persistent reference to the model;
 * it receives it as a parameter when a full snapshot is needed.
 * </p>
 */
public class ViewUpdater {

    private final List<GameEventListener> listeners;

    /**
     * Constructs a new ViewUpdater.
     * Initializes a thread-safe list of listeners to prevent concurrent modification exceptions
     * during network broadcasts.
     */
    public ViewUpdater() {
        this.listeners = new CopyOnWriteArrayList<>();
    }

    /**
     * Registers a new listener to receive game updates.
     * Prevents duplicate registrations.
     *
     * @param listener the {@link GameEventListener} to add (usually a ClientHandler)
     */
    public void addListener(GameEventListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Removes a listener so it no longer receives game updates.
     *
     * @param listener the {@link GameEventListener} to remove
     */
    public void removeListener(GameEventListener listener) {
        listeners.remove(listener);
    }

    /**
     * Utility method to send a message to all registered listeners.
     *
     * @param message the network message to broadcast
     */
    private void broadcast(Object message) {
        for (GameEventListener listener : listeners) {
            listener.onGameUpdate(message);
        }
    }

    /**
     * Maps the  {@link GameModel} into a safe, serializable {@link GameDTO}.
     * Extracts players' status, tribes, board cards, and offer track.
     *
     * @param model the current state of the game model
     * @return a Data Transfer Object representing the current game snapshot
     */
    private GameDTO buildGameDTO(GameModel model) {

        int numberOfPlayers = model.getNumPlayers();
        int currentPlayerIndex = model.getPlayers().indexOf(model.getCurrentPlayer());

        List<PlayerDTO> playersDTOList = new ArrayList<>();
        for (Player p : model.getPlayers()) {


            List<CardDTO> tribeDTO = new ArrayList<>();

            for (Card c : p.getTribe().getCharacters()) {
                tribeDTO.add(c.toDTO());
            }

            for (Card c : p.getTribe().getBuildings()){
                tribeDTO.add(c.toDTO());
            }

            PlayerDTO pDTO = new PlayerDTO(
                    p.getPlayerId(),
                    p.getNickname(),
                    p.getTotemColor(),
                    p.getFood(),
                    p.getPrestigePoints(),
                    tribeDTO
            );
            playersDTOList.add(pDTO);
        }

        List<CardDTO> upperRowDTO = new ArrayList<>();
        for (Card c : model.getAvailableCardsFromRow(0)) {
            upperRowDTO.add(c.toDTO());
        }

        List<CardDTO> lowerRowDTO = new ArrayList<>();
        for (Card c : model.getAvailableCardsFromRow(1)) {
            lowerRowDTO.add(c.toDTO());
        }

        List<OfferTileDTO> offerTrackDTO = new ArrayList<>();
        for (OfferTile t : model.getBoard().getOfferTrack().getAvailableTiles()) {
            offerTrackDTO.add(t.toDTO());
        }

        return new GameDTO(
                numberOfPlayers,
                currentPlayerIndex,
                playersDTOList,
                upperRowDTO,
                lowerRowDTO,
                offerTrackDTO
        );
    }

    // --- Game lifecycle notifications ---

    /**
     * Sends a game-started notification to all listeners.
     * Called at game start after setupGame() has completed.
     *
     * @param model the initialized game model to be broadcasted
     */
    public void notifyGameStarted(GameModel model) {
        broadcast(new GameSnapshotMessage(buildGameDTO(model)));
    }

    /**
     * Sends a full game snapshot to a single, specific listener.
     * Usually used for synchronization when a client reconnects.
     *
     * @param listener the target listener
     * @param model    the current game model
     */
    public void sendFullSnapshotTo(GameEventListener listener, GameModel model) {
        listener.onGameUpdate(new GameSnapshotMessage(buildGameDTO(model)));
    }

    // --- Phase and turn notifications ---

    /**
     * Broadcasts a phase change to all clients.
     *
     * @param newPhase the new {@link GamePhase} the game has entered
     */
    public void notifyPhaseChanged(GamePhase newPhase) {
        broadcast(new PhaseUpdateMessage(newPhase));
    }

    /**
     * Broadcasts a turn change, indicating which player is now active.
     *
     * @param p the {@link Player} whose turn has just started
     */
    public void notifyPlayerTurn(Player p) {
        broadcast(
                new TurnUpdateMessage(p.getNickname(), p.getPlayerId()));
    }

    // --- Positioning phase ---

    /**
     * Broadcasts an updated game snapshot after a player has placed their totem.
     *
     * @param model the updated game model
     */
    public void notifyTotemPlaced(GameModel model) {
        broadcast(new GameSnapshotMessage(buildGameDTO(model)));
    }

    // --- Action resolution phase notifications ---

    /**
     * Sends a notification to a specific player confirming they have taken a card.
     *
     * @param playerId the ID of the player who took the card
     * @param c        the {@link Card} taken by the player
     */
    public void notifyCardTaken(int playerId, Card c) {
        for(GameEventListener listener : listeners) {
            if(listener.getPlayerId() == playerId) {
                listener.onGameUpdate(new CardTakenMessage(playerId, c.toDTO()));
            }
        }
    }

    /**
     * Sends a notification to a specific player updating their food amount.
     *
     * @param playerId   the ID of the player who took the food
     * @param playerFood the updated amount of food owned by the player
     */
    public void notifyFoodTaken(int playerId, int playerFood) {
        for(GameEventListener listener : listeners) {
            if(listener.getPlayerId() == playerId) {
                listener.onGameUpdate(new FoodTakenMessage(playerFood));
            }
        }
    }

    /**
     * Placeholder method for notifying when a totem is returned.
     * Currently unimplemented.
     */
    public void notifyTotemReturned() {}

    // --- End-of-round notifications ---

    /**
     * Broadcasts the resolution of an event card to all players.
     *
     * @param type  the type or category of the resolved event
     * @param front the description or effect printed on the front of the event card
     */
    public void notifyEventResolved(String type, String front) {
        broadcast(new EventResolvedMessage(type, front));
    }

    /**
     * Broadcasts the end of the current round to all players.
     *
     * @param currentRound the number of the completed round
     * @param eraChanged   true if the new round starts a new Era, false otherwise
     * @param currentEra   the string representation of the current/new Era
     */
    public void notifyRoundEnded(int currentRound, boolean eraChanged, String currentEra) {
        broadcast(new RoundEndedMessage(currentRound, eraChanged, currentEra));
    }

    // --- Game over ---

    /**
     * Broadcasts the end of the game, including a closing message and the final board state.
     *
     * @param GameOverString a message indicating the game is over (e.g., winner announcement)
     * @param model          the final state of the game model
     */
    public void notifyGameOver(String GameOverString, GameModel model) {
        broadcast(new GameOverMessage(GameOverString, buildGameDTO(model)));
    }

    // --- Error notifications ---

    /**
     * Sends an error message privately to a specific player (e.g., for an invalid action).
     *
     * @param errorDetail the description of the error
     * @param playerId    the ID of the player who caused the error and should receive the message
     */
    public void notifyError(int playerId, String error, String errorDetail ) {
        for(GameEventListener listener : listeners) {
            if(listener.getPlayerId() == playerId) {
                listener.onGameUpdate(new ErrorMessage(error,errorDetail));
            }
        }
    }

    // --- Lobby notifications ---

    /**
     * Broadcasts a lobby update to all clients currently waiting for the game to start.
     *
     * @param currentPlayers  the number of players currently connected
     * @param expectedPlayers the target number of players required to start the game
     */
    public void notifyLobbyUpdate(int currentPlayers, int expectedPlayers) {
        broadcast(new LobbyUpdateMessage(currentPlayers, expectedPlayers));
    }

    // --- Connection notifications ---

    /**
     * Broadcasts a notification that a player has disconnected from the game.
     *
     * @param playerNickname the nickname of the disconnected player
     */
    public void notifyPlayerDisconnected(String playerNickname) {
        broadcast(new ConnectionUpdateMessage(playerNickname, false));
    }

    /**
     * Broadcasts a notification that a previously disconnected player has rejoined the game.
     *
     * @param playerNickname the nickname of the reconnected player
     */
    public void notifyPlayerReconnected(String playerNickname) {
        broadcast(new ConnectionUpdateMessage(playerNickname, true));
    }
}