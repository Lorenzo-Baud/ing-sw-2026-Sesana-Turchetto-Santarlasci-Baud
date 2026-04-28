package it.polimi.ingws.gc8.Network;

import it.polimi.ingws.gc8.Controller.events.GameEventListener;
import it.polimi.ingws.gc8.Model.Board.OfferTile;
import it.polimi.ingws.gc8.Model.Cards.Card;
import it.polimi.ingws.gc8.Model.Enum.CardRow;
import it.polimi.ingws.gc8.Model.Enum.Era;
import it.polimi.ingws.gc8.Model.Enum.GamePhase;
import it.polimi.ingws.gc8.Model.GameModel;
import it.polimi.ingws.gc8.Model.Player;
import it.polimi.ingws.gc8.Network.DTO.CardDTO;
import it.polimi.ingws.gc8.Network.DTO.DtoMapper;
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
     * Utility method to send a message to all registered listeners.
     *
     * @param message the network message to broadcast
     */
    private void broadcast(NetworkMessage message) {
        for (GameEventListener listener : listeners) {
            listener.onGameUpdate(message);
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
     * @param model the network message to broadcast
     */
    public void broadcastModel(GameModel model) {
        ModelUpdateMessage modelUpdate = new ModelUpdateMessage(buildGameDTO(model));
        for (GameEventListener listener : listeners) {
            listener.onGameUpdate(modelUpdate);
        }
    }

    /**
     * Sends a full game snapshot to a single listener (used during reconnection
     * to bring a returning client back in sync without disturbing the others).
     *
     * @param listener the target listener (typically the reconnecting client handler)
     * @param model    the current game model
     */
    public void sendFullSnapshotTo(GameEventListener listener, GameModel model) {
        if (listener == null || model == null) return;
        listener.onGameUpdate(new ModelUpdateMessage(buildGameDTO(model)));
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
            playersDTOList.add(DtoMapper.toDTO(p));
        }

        List<CardDTO> upperRowDTO = new ArrayList<>();
        for (Card c : model.getAvailableCardsFromRow(CardRow.UPPER)) {
            upperRowDTO.add(DtoMapper.toDTO(c));
        }

        List<CardDTO> lowerRowDTO = new ArrayList<>();
        for (Card c : model.getAvailableCardsFromRow(CardRow.LOWER)) {
            lowerRowDTO.add(DtoMapper.toDTO(c));
        }

        List<OfferTileDTO> offerTrackDTO = new ArrayList<>();
        for (OfferTile t : model.getBoard().getOfferTrack().getAvailableTiles()) {
            offerTrackDTO.add(DtoMapper.toDTO(t));
        }

        PlayerDTO currentPlayer = null;
        if (model.getCurrentPlayer() != null) {
            currentPlayer = DtoMapper.toDTO(model.getCurrentPlayer());
        }

        GamePhase phase = model.getPhase();

        int currentRound = model.getCurrentRound();

        Era currentEra = model.getCurrentEra();

        List<PlayerDTO> turnOrderTrackDTO = new ArrayList<>();
        if (model.getBoard() != null && model.getBoard().getTurnOrderTrack() != null) {
            for (Player p : model.getBoard().getTurnOrderTrack().getCurrentOrder()) {
                turnOrderTrackDTO.add(DtoMapper.toDTO(p));
            }
        }

        return new GameDTO(
                numberOfPlayers,
                currentPlayerIndex,
                playersDTOList,
                upperRowDTO,
                lowerRowDTO,
                offerTrackDTO,
                currentPlayer,
                phase,
                currentRound,
                currentEra,
                turnOrderTrackDTO
        );
    }

    // --- Game lifecycle notifications ---





    /**
     * Broadcasts a turn change, indicating which player is now active.
     *
     * @param p the {@link Player} whose turn has just started
     */
    public void notifyPlayerTurn(Player p) {
        broadcast(new TurnUpdateMessage(p.getNickname(), p.getPlayerId()));
    }


    // --- Game over ---

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
     * @param nicknames       the nicknames currently in the lobby
     */
    public void notifyLobbyUpdate(int currentPlayers, int expectedPlayers,
                                  java.util.List<String> nicknames) {
        broadcast(new LobbyUpdateMessage(currentPlayers, expectedPlayers, nicknames));
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