package it.polimi.ingws.gc8.View.GUI;

import it.polimi.ingws.gc8.Model.Enum.TotemColor;
import it.polimi.ingws.gc8.Network.Messages.ConnectionUpdateMessage;
import it.polimi.ingws.gc8.Network.Messages.ErrorMessage;
import it.polimi.ingws.gc8.Network.Messages.LobbyUpdateMessage;
import it.polimi.ingws.gc8.Network.Messages.ModelUpdateMessage;
import it.polimi.ingws.gc8.Network.Messages.NetworkMessage;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.util.List;
import java.util.logging.Logger;

/**
 * JavaFX controller for the lobby screen.
 *
 * <p>Runs the first GUI dispatcher of the session: it consumes lobby updates
 * from {@code View.messages} and, on the first {@link ModelUpdateMessage},
 * transitions to the game screen. The dispatcher returns only after handing
 * the queue over to {@code GameScreenController} (which starts its own
 * dispatcher in its {@code initialize()}).
 */
public class LobbyScreenController {

    private static final Logger LOGGER = Logger.getLogger(LobbyScreenController.class.getName());

    /** "current / expected" connected-players counter. */
    @FXML private Label playerCountLabel;
    /** Container the connected players and the empty waiting slots are rendered into. */
    @FXML private VBox playerListBox;

    /** Pulsing dots shown while waiting for the other players. */
    @FXML private Circle lobbyDot1;
    @FXML private Circle lobbyDot2;
    @FXML private Circle lobbyDot3;

    /** Decorative totem colours used for the lobby roster, assigned by join order.
     *  (Real in-game colours are assigned at game start; these are cosmetic.) */
    private static final TotemColor[] LOBBY_TOTEMS = {
            TotemColor.RED, TotemColor.BLUE, TotemColor.YELLOW, TotemColor.BLACK, TotemColor.WHITE
    };

    /** Reference to the main view manager to coordinate scene switches. */
    private GuiView guiView;

    /** Setter used by GuiView to inject itself after loading the FXML. */
    public void setGuiView(GuiView guiView) {
        this.guiView = guiView;
    }

    @FXML
    public void initialize() {
        guiView.getPrimaryStage().setTitle("Mesos Game - Attesa giocatori");
        animateDot(lobbyDot1, 0);
        animateDot(lobbyDot2, 200);
        animateDot(lobbyDot3, 400);
        startDispatcher();
    }

    /** Starts a looping fade pulse on a loading dot, offset by {@code delayMs}. */
    private void animateDot(Node dot, int delayMs) {
        if (dot == null) return;
        FadeTransition pulse = new FadeTransition(Duration.millis(700), dot);
        pulse.setFromValue(0.35);
        pulse.setToValue(1.0);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(FadeTransition.INDEFINITE);
        pulse.setDelay(Duration.millis(delayMs));
        pulse.play();
    }

    /**
     * Starts the lobby dispatcher thread. The thread is a daemon and exits
     * as soon as the lobby phase ends (first ModelUpdateMessage or fatal
     * error); the game screen controller will own the queue from then on.
     */
    private void startDispatcher() {
        Thread updateDispatcher = new Thread(this::dispatchLoop, "gui-dispatcher-lobby");
        updateDispatcher.setDaemon(true);
        updateDispatcher.start();
    }

    private void dispatchLoop() {
        while (true) {
            NetworkMessage msg = guiView.waitForMessage();
            switch (msg) {
                case null -> {
                    // Interrupted: nothing to do, let the thread die quietly.
                    return;
                }
                case ModelUpdateMessage m -> {
                    // First game snapshot: store it and hand off to the game screen.
                    // GameScreenController.initialize() will start its own dispatcher.
                    guiView.setGameView(m.getGameModelDTO());
                    guiView.showGame();
                    return;
                }
                case LobbyUpdateMessage m -> Platform.runLater(() ->
                        renderRoster(m.getCurrentPlayers(), m.getExpectedPlayers(), m.getNicknames()));
                case ErrorMessage m -> {
                    guiView.showError("Errore", m.getError());
                    return;
                }
                case ConnectionUpdateMessage m -> {
                    String who = m.getNickname() == null ? "?" : m.getNickname();
                    LOGGER.fine(() -> who + (m.isConnected() ? " reconnected." : " disconnected."));
                }
                default -> {
                    // Ignore other message types in the lobby screen.
                }
            }
        }
    }

    /**
     * Rebuilds the connected-players roster: one row per joined player (totem,
     * name, "TU" badge for yourself, "pronto" status) followed by dashed
     * "In attesa..." placeholders up to {@code expected}. Must run on the FX thread.
     */
    private void renderRoster(int current, int expected, List<String> nicknames) {
        playerCountLabel.setText(current + " / " + expected);
        playerListBox.getChildren().clear();
        if (nicknames == null) nicknames = List.of();

        for (int i = 0; i < nicknames.size(); i++) {
            playerListBox.getChildren().add(buildPlayerRow(i, nicknames.get(i)));
        }
        for (int i = nicknames.size(); i < expected; i++) {
            playerListBox.getChildren().add(buildWaitingRow());
        }
    }

    /** A connected-player row: totem icon, name, optional "TU" badge, "pronto". */
    private HBox buildPlayerRow(int index, String nickname) {
        HBox row = new HBox(11);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("lobby-row");

        TotemColor color = LOBBY_TOTEMS[index % LOBBY_TOTEMS.length];
        ImageView totem = new ImageView(GuiAssets.totemFlat(color));
        totem.setFitHeight(30);
        totem.setPreserveRatio(true);
        totem.setSmooth(true);

        Label name = new Label(nickname);
        name.getStyleClass().add("lobby-name");
        row.getChildren().addAll(totem, name);

        if (nickname != null && nickname.equals(guiView.getNickname())) {
            Label you = new Label("TU");
            you.getStyleClass().add("lobby-you-badge");
            row.getChildren().add(you);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label ready = new Label("pronto");
        ready.getStyleClass().add("lobby-ready");
        row.getChildren().addAll(spacer, ready);
        return row;
    }

    /** An empty placeholder row: dashed circle + muted "In attesa..." text. */
    private HBox buildWaitingRow() {
        HBox row = new HBox(11);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("lobby-row");

        Circle slot = new Circle(15);
        slot.setFill(Color.TRANSPARENT);
        slot.setStroke(Color.web("#6B5D54"));
        slot.setStrokeWidth(1.5);
        slot.getStrokeDashArray().addAll(3.0, 4.0);

        Label waiting = new Label("In attesa...");
        waiting.getStyleClass().add("lobby-waiting");

        row.getChildren().addAll(slot, waiting);
        return row;
    }

    /** "Abbandona" button: closes the connection and returns to the start screen. */
    @FXML
    private void handleLeave() {
        try {
            guiView.getConnection().disconnect();
        } catch (Exception e) {
            LOGGER.fine(() -> "Disconnect on leave failed: " + e.getMessage());
        }
        guiView.showStartScreen();
    }
}
