package it.polimi.ingws.gc8.View.GUI;

import it.polimi.ingws.gc8.Controller.actions.*;
import it.polimi.ingws.gc8.Model.Board.OffertileId;
import it.polimi.ingws.gc8.Model.Enum.*;
import it.polimi.ingws.gc8.Network.DTO.CardDTO;
import it.polimi.ingws.gc8.Network.DTO.GameDTO;
import it.polimi.ingws.gc8.Network.DTO.OfferTileDTO;
import it.polimi.ingws.gc8.Network.DTO.PlayerDTO;
import it.polimi.ingws.gc8.Network.Messages.ConnectionUpdateMessage;
import it.polimi.ingws.gc8.Network.Messages.ErrorMessage;
import it.polimi.ingws.gc8.Network.Messages.EventResolvedMessage;
import it.polimi.ingws.gc8.Network.Messages.GameOverMessage;
import it.polimi.ingws.gc8.Network.Messages.ModelUpdateMessage;
import it.polimi.ingws.gc8.Network.Messages.NetworkMessage;
import it.polimi.ingws.gc8.View.View;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.util.Duration;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * JavaFX controller for the redesigned game screen.
 *
 * <p>Owns the per-game dispatcher thread that drains {@code View.messages} and
 * re-renders the scene on the FX thread when a {@code ModelUpdateMessage}
 * arrives. All UI mutations are confined to the JavaFX Application Thread.
 *
 * <p>Render rule: every render reads {@code guiView.getGameView()} <b>once</b>
 * into a local snapshot and uses it for the whole frame.
 */
public class GameScreenController {

    private static final Logger LOGGER = Logger.getLogger(GameScreenController.class.getName());
    private static final double CARD_CORNER_RADIUS = 16.0;
    /** Card artwork aspect ratio (width / height). Upper/lower cards are
     *  840x1280 (~0.656); offer tiles are narrower, so this is the widest case. */
    private static final double CARD_ASPECT_RATIO = 0.66;
    /** Horizontal gap between cards in a row; must match the HBox spacing in the FXML. */
    private static final double CARD_ROW_SPACING = 13.0;

    /** Variables to track how many cards have been taken from each row during the current turn, to enforce quotas. */
    int turnUpperTaken = 0;
    int turnLowerTaken = 0;
    boolean turnFoodTaken = false;

    /** Last card the player attempted to draw, so a server rejection can flash
     *  the denied feedback on exactly that slot. {@code -1} when none pending. */
    private CardRow lastAttemptedRow = null;
    private int lastAttemptedIndex = -1;

    /** Reference to the owning GuiView, injected before {@code load()}. */
    private GuiView guiView;

    // ---- Root & layout ----
    @FXML private StackPane rootPane;
    @FXML private VBox marketBox;
    /** Card artwork height, derived from the available market height so cards
     *  scale with the window and never need per-card scrolling. */
    private DoubleBinding cardHeight;
    /** Largest number of cards currently shown in a single market row. Used to
     *  cap card width so a crowded row never overflows the centered HBox and
     *  clips on the sides. Updated each render. */
    private final javafx.beans.property.IntegerProperty maxCardsInRow =
            new javafx.beans.property.SimpleIntegerProperty(9);

    // ---- HUD ----
    @FXML private Label eraLabel;
    @FXML private Label roundLabel;
    @FXML private HBox turnPill;
    @FXML private Circle turnDot;
    /** Center turn pill text ("Tocca a te" / "Turno di …"). */
    @FXML private Label yourTurnLabel;

    // ---- Players strip & turn-order rail (built dynamically) ----
    @FXML private HBox playersStrip;
    @FXML private VBox turnOrderList;

    // ---- Resolved-events log (bottom of the left rail) ----
    @FXML private VBox eventLogBox;

    // ---- Market rows ----
    @FXML private HBox topCardsRow;
    @FXML private HBox middleCardsRow;
    @FXML private HBox bottomCardsRow;

    // ---- Tribe mini-box (action bar) ----
    @FXML private Label foodLabel;
    @FXML private Label prestigeLabel;
    @FXML private Label buildingDiscountLabel;

    // ---- Action bar ----
    @FXML private Label quotaLabel;

    /** Injects the owning {@link GuiView} instance. */
    public void setGuiView(GuiView guiView) {
        this.guiView = guiView;
    }

    /**
     * Runs on the JavaFX Application Thread. Performs the first render from
     * the snapshot already stored in {@code guiView.getGameView()} and starts
     * the dispatcher thread that consumes subsequent {@code NetworkMessage}s.
     */
    @FXML
    public void initialize() {
        guiView.getPrimaryStage().setTitle("Mesos Game - Gioco");

        // Cards size to the smaller of two budgets so they always fit:
        //  - vertical: the available market height split across the three rows;
        //  - horizontal: the available width divided by the busiest row's card
        //    count (else a crowded centered HBox overflows and clips at the sides).
        cardHeight = Bindings.createDoubleBinding(
                () -> {
                    double vertical = marketBox.getHeight() / 3.0 - 46.0;
                    int n = Math.max(1, maxCardsInRow.get());
                    // marketBox left+right padding is 22+22; keep a little safety slack.
                    double availWidth = marketBox.getWidth() - 44.0 - 16.0;
                    double perCardWidth = (availWidth - CARD_ROW_SPACING * (n - 1)) / n;
                    double horizontal = perCardWidth / CARD_ASPECT_RATIO;
                    return Math.max(90.0, Math.min(vertical, horizontal));
                },
                marketBox.heightProperty(), marketBox.widthProperty(), maxCardsInRow);

        // First render from the existing snapshot (we're already on the FX thread).
        renderFromSnapshot(guiView.getGameView());

        // Start the per-game dispatcher: the lobby dispatcher returned right
        // after calling showGame(), so the queue would otherwise have no consumer.
        Thread updateDispatcher = new Thread(this::dispatchLoop, "gui-dispatcher-game");
        updateDispatcher.setDaemon(true);
        updateDispatcher.start();
    }

    /**
     * Per-frame render. Reads {@code guiView.getGameView()} once and uses the
     * same snapshot for the whole frame. Must be called on the FX thread.
     */
    private void renderFromSnapshot(GameDTO snap) {
        if (snap == null) return;
        final int n = snap.numberOfPlayers();
        final int me = guiView.getPlayerId();
        final boolean myTurn = guiView.isMyTurn();

        // ---------- HUD ----------
        eraLabel.setText("ERA " + roman(snap.currentEra().ordinal() + 1));
        roundLabel.setText("Round " + snap.currentRound() + " / 10");
        renderTurnPill(snap, myTurn);

        // ---------- PLAYERS STRIP ----------
        playersStrip.getChildren().clear();
        for (int i = 0; i < n; i++) {
            PlayerDTO p = snap.players().get(i);
            boolean active = snap.currentPlayer() != null
                    && snap.currentPlayer().nickname() != null
                    && snap.currentPlayer().nickname().equals(p.nickname());
            boolean isYou = p.playerId() == me;
            playersStrip.getChildren().add(buildPlayerCard(snap, p, active, isYou));
        }

        // ---------- LEFT RAIL (turn order) ----------
        renderTurnOrder(snap, n);

        // ---------- TRIBE MINI-BOX (cibo + prestigio + sconto edifici) ----------
        PlayerDTO localPlayer = playerById(snap, me);
        if (localPlayer != null) {
            TribeStats localStats = tribeStats(localPlayer);
            foodLabel.setText(String.valueOf(localPlayer.foodPoints()));
            prestigeLabel.setText(String.valueOf(localPlayer.prestigePoints()));
            buildingDiscountLabel.setText(localStats.builderDiscount() == 0
                    ? "0"
                    : "-" + localStats.builderDiscount() + "F");
        } else {
            foodLabel.setText("0");
            prestigeLabel.setText("0");
            buildingDiscountLabel.setText("0");
        }

        // ---------- MARKET ROWS ----------
        topCardsRow.getChildren().clear();
        middleCardsRow.getChildren().clear();
        bottomCardsRow.getChildren().clear();

        for (int i = 0; i < snap.upperRowCards().size(); i++) {
            addCardToRow(CardRow.UPPER, snap.upperRowCards().get(i), i);
        }
        for (int i = 0; i < snap.offerTrack().size(); i++) {
            OfferTileDTO tile = snap.offerTrack().get(i);
            TotemColor occupantColor = null;
            if (tile.playerIndexOccupant() != -1) {
                occupantColor = snap.players().get(tile.playerIndexOccupant() - 1).totemColor();
            }
            addCardToOfferTrack(tile, occupantColor);
        }
        for (int i = 0; i < snap.lowerRowCards().size(); i++) {
            addCardToRow(CardRow.LOWER, snap.lowerRowCards().get(i), i);
        }

        // Cap card width to the busiest row so a crowded centered row never clips.
        maxCardsInRow.set(Math.max(snap.upperRowCards().size(),
                Math.max(snap.offerTrack().size(), snap.lowerRowCards().size())));

        // ---------- ACTION BAR ----------
        renderActionBar(snap, myTurn);
    }

    /** Updates the central HUD pill describing whose turn it is. */
    private void renderTurnPill(GameDTO snap, boolean myTurn) {
        if (myTurn) {

            if(myOfferTile(guiView.getGameView()) == null) {
                yourTurnLabel.setText("Tocca a te · piazza il totem sul tracciato delle offerte");
            } else {
                yourTurnLabel.setText("Tocca a te · pesca le carte dalle file");
            }

            turnDot.setStyle("-fx-fill: #E2592B;");
            setStyleClass(turnPill, "pill-accent");
        } else {
            String who = snap.currentPlayer() != null ? snap.currentPlayer().nickname() : "—";
            yourTurnLabel.setText("Turno di " + who);
            turnDot.setStyle("-fx-fill: #B7A892;");
            setStyleClass(turnPill, "pill");
        }
    }

    /** Builds one card of the players strip. */
    private HBox buildPlayerCard(GameDTO snap, PlayerDTO p, boolean active, boolean isYou) {
        HBox card = new HBox(12);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add(active ? "player-card-active" : "player-card");
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setMaxWidth(Double.MAX_VALUE);

        ImageView totem = new ImageView(GuiAssets.totem3D(p.totemColor()));
        totem.setFitHeight(42);
        totem.setPreserveRatio(true);

        VBox info = new VBox(3);
        HBox nameRow = new HBox(7);
        nameRow.setAlignment(Pos.CENTER_LEFT);
        Label name = new Label(isYou ? "Tu" : p.nickname());
        name.getStyleClass().add("player-name");
        nameRow.getChildren().add(name);
        if (active) {
            Label badge = new Label("IN GIOCO");
            badge.getStyleClass().add("badge-active");
            nameRow.getChildren().add(badge);
        }

        Label meta = new Label();
        meta.getStyleClass().add("player-meta");
        Label pp = new Label(String.valueOf(p.prestigePoints()));
        pp.getStyleClass().addAll("player-meta", "player-pp");
        // meta text: "<pp> PP · <state>"
        meta.setText(" PP · " + playerStateText(snap, p, active));
        HBox metaRow = new HBox(0, pp, meta);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        info.getChildren().addAll(nameRow, metaRow);
        card.getChildren().addAll(totem, info);

        // For opponents, add a peek arrow on the right that opens their tribe.
        // Tribes are public in MESOS (cards lie face-up), so this reveals no
        // hidden state. The local player's own card is left unchanged — they
        // already have the tribe mini-box in the action bar.
        if (!isYou) {
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Label peek = new Label("›");
            peek.getStyleClass().add("tribe-peek-arrow");
            Tooltip.install(peek, new Tooltip("Vedi la tribù di " + p.nickname()));
            peek.setOnMouseClicked(e -> showTribePopup(p, false));
            card.getChildren().addAll(spacer, peek);
        }
        return card;
    }

    /** Short status fragment shown under a player's name in the strip. */
    private String playerStateText(GameDTO snap, PlayerDTO p, boolean active) {
        if (active) return "in azione";
        List<PlayerDTO> track = snap.turnOrderTrack();
        for (int i = 0; i < track.size(); i++) {
            PlayerDTO t = track.get(i);
            if (t != null && t.nickname() != null && t.nickname().equals(p.nickname())) {
                return "ordine " + (i + 1) + "°";
            }
        }
        return "in attesa";
    }

    /** Renders the left turn-order rail: one slot per board position. */
    private void renderTurnOrder(GameDTO snap, int n) {
        turnOrderList.getChildren().clear();
        String[] bonuses = turnOrderBonuses(n);
        List<PlayerDTO> track = snap.turnOrderTrack();

        Image trackImage = GuiAssets.image("/cards/" + n + "player.png");
        if (trackImage != null) {
            StackPane trackCard = buildTurnOrderCard(trackImage, track, n);
            turnOrderList.getChildren().add(trackCard);
            addTurnOrderLegend(track, bonuses);
            return;
        }

        for (int i = 0; i < n; i++) {
            PlayerDTO occupant = (track != null && i < track.size()) ? track.get(i) : null;

            HBox slot = new HBox(9);
            slot.setAlignment(Pos.CENTER_LEFT);
            slot.getStyleClass().add(occupant != null ? "turn-slot" : "turn-slot-empty");

            Label idx = new Label(String.valueOf(i + 1));
            idx.getStyleClass().add("turn-slot-index");
            idx.setMinWidth(12);

            VBox text = new VBox(1);
            HBox.setHgrow(text, Priority.ALWAYS);
            Label nameOrBonus = new Label(occupant != null ? occupant.nickname() : "—");
            nameOrBonus.getStyleClass().add("turn-slot-name");
            text.getChildren().add(nameOrBonus);
            if (bonuses[i] != null) {
                Label bonus = new Label(bonuses[i]);
                bonus.getStyleClass().add("turn-slot-bonus");
                text.getChildren().add(bonus);
            }

            slot.getChildren().add(idx);
            if (occupant != null) {
                ImageView totem = new ImageView(GuiAssets.totemFlat(occupant.totemColor()));
                totem.setFitHeight(26);
                totem.setPreserveRatio(true);
                slot.getChildren().add(totem);
            }
            slot.getChildren().add(text);
            turnOrderList.getChildren().add(slot);
        }
    }

    /** Builds the graphic turn-order card and overlays player totems on its slots. */
    private StackPane buildTurnOrderCard(Image trackImage, List<PlayerDTO> track, int n) {
        ImageView trackView = new ImageView(trackImage);
        trackView.fitWidthProperty().bind(Bindings.createDoubleBinding(
                () -> Math.max(120.0, turnOrderList.getWidth()),
                turnOrderList.widthProperty()));
        trackView.setPreserveRatio(true);
        trackView.setSmooth(true);
        clipRoundedToFitWidth(trackView);

        Pane markerLayer = new Pane();
        markerLayer.setMouseTransparent(true);
        markerLayer.prefWidthProperty().bind(trackView.fitWidthProperty());
        DoubleBinding trackHeight = trackView.fitWidthProperty().multiply(trackImage.getHeight() / trackImage.getWidth());
        markerLayer.prefHeightProperty().bind(trackHeight);

        double[] centersY = turnOrderSlotCenters(n);
        for (int i = 0; i < Math.min(n, centersY.length); i++) {
            PlayerDTO occupant = (track != null && i < track.size()) ? track.get(i) : null;
            if (occupant == null) continue;

            Image totemImage = GuiAssets.totemFlat(occupant.totemColor());
            ImageView totem = new ImageView(totemImage);
            DoubleBinding totemHeight = trackView.fitWidthProperty().multiply(0.20);
            totem.fitHeightProperty().bind(totemHeight);
            totem.setPreserveRatio(true);
            totem.setSmooth(true);
            double aspect = totemImage.getWidth() / totemImage.getHeight();
            DoubleBinding totemWidth = totemHeight.multiply(aspect);
            totem.layoutXProperty().bind(trackView.fitWidthProperty().multiply(0.50).subtract(totemWidth.multiply(0.50)));
            totem.layoutYProperty().bind(trackHeight.multiply(centersY[i]).subtract(totemHeight.multiply(0.75)));
            markerLayer.getChildren().add(totem);
        }

        StackPane card = new StackPane(trackView, markerLayer);
        card.getStyleClass().add("turn-order-card");
        card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }

    /** Adds the compact textual legend below the graphic turn-order card. */
    private void addTurnOrderLegend(List<PlayerDTO> track, String[] bonuses) {
        if (track == null) return;
        for (int i = 0; i < track.size(); i++) {
            PlayerDTO p = track.get(i);
            if (p == null) continue;

            HBox row = new HBox(6);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("turn-order-legend");

            ImageView totem = new ImageView(GuiAssets.totemFlat(p.totemColor()));
            totem.setFitHeight(15);
            totem.setPreserveRatio(true);

            String bonus = (i < bonuses.length && bonuses[i] != null) ? " · " + bonuses[i] : "";
            Label text = new Label(p.nickname() + " · " + (i + 1) + "°" + bonus);
            text.getStyleClass().add("turn-order-legend-text");

            row.getChildren().addAll(totem, text);
            turnOrderList.getChildren().add(row);
        }
    }

    /** Slot centers on the turn-order artwork, expressed as fractions of image height. */
    private static double[] turnOrderSlotCenters(int n) {
        return switch (n) {
            case 2 -> new double[] {0.33, 0.51};
            case 3 -> new double[] {0.27, 0.46, 0.65};
            case 4 -> new double[] {0.21, 0.40, 0.59, 0.78};
            case 5 -> new double[] {0.14, 0.33, 0.52, 0.71, 0.90};
            default -> new double[0];
        };
    }

    /** Per-position turn-order bonus labels, indexed by slot, for {@code n} players. */
    private String[] turnOrderBonuses(int n) {
        String[] b = new String[n];
        switch (n) {
            case 2 -> { b[0] = "+1 cibo"; b[1] = "-1 cibo / -2 PP"; }
            case 3 -> { b[0] = "+2 cibo"; b[2] = "-1 cibo / -2 PP"; }
            case 4 -> { b[0] = "+2 cibo"; b[1] = "+1 cibo"; b[3] = "-1 cibo / -2 PP"; }
            case 5 -> { b[0] = "+3 cibo"; b[1] = "+1 cibo"; b[4] = "-1 cibo / -2 PP"; }
            default -> { /* no bonuses */ }
        }
        return b;
    }

    /** Updates the action bar quota text. */
    private void renderActionBar(GameDTO snap, boolean myTurn) {
        OfferTileDTO myTile = myOfferTile(snap);
        if (myTurn && myTile != null) {
            int maxUpper = View.maxUpperPicks(myTile.id());
            int maxLower = View.maxLowerPicks(myTile.id());
            quotaLabel.setText("↑ Superiore " + turnUpperTaken + "/" + maxUpper
                    + "   ·   ↓ Inferiore " + turnLowerTaken + "/" + maxLower);
        } else if (myTurn) {
            quotaLabel.setText("Posiziona il tuo totem su una tessera offerta");
        } else {
            quotaLabel.setText("In attesa del tuo turno");
        }
    }

    /** Convenience overload that reads the current snapshot. */
    private void updateInterface() {
        GameDTO snap = guiView.getGameView();
        renderFromSnapshot(snap);

        if (snap != null && guiView.isMyTurn()) {
            OfferTileDTO myTile = myOfferTile(snap);
            if (myTile != null) {

                // If the player placed their totem on the food tile, take food and
                // return to the turn order track.
                if (myTile.id() == OffertileId.A && !turnFoodTaken) {
                    turnFoodTaken = true;
                    if (guiView.sendAndAwaitUpdate(new TakeFoodAction(guiView.getPlayerId())) == View.SendResult.OK) {
                        endTurnAndReturnTotem();
                    } else {
                        turnFoodTaken = false;
                    }
                }

                // If the player cannot take any more cards from the rows, end the turn.
                boolean canPick = false;

                int remainingUpper = View.maxUpperPicks(myTile.id()) - turnUpperTaken;
                int remainingLower = View.maxLowerPicks(myTile.id()) - turnLowerTaken;
                PlayerDTO localPlayer = playerById(snap, guiView.getPlayerId());

                if (remainingUpper > 0) {
                    canPick = hasPickableCard(snap.upperRowCards(), localPlayer);
                }

                if (!canPick && remainingLower > 0) {
                    canPick = hasPickableCard(snap.lowerRowCards(), localPlayer);
                }

                if (!canPick) {
                    endTurnAndReturnTotem();
                }

            }
        }
    }

    /**
     * Continuously waits for network messages and dispatches UI updates.
     * Runs on the {@code gui-dispatcher-game} thread; all FX-thread work is
     * wrapped in {@link Platform#runLater}.
     */
    private void dispatchLoop() {
        while (true) {
            NetworkMessage msg = guiView.waitForMessage();
            switch (msg) {
                case null -> {
                    guiView.showError("Errore", "Connessione al server persa.");
                    return;
                }
                case ModelUpdateMessage m -> {
                    guiView.setGameView(m.getGameModelDTO());
                    Platform.runLater(this::updateInterface);
                }
                case GameOverMessage m -> Platform.runLater(() -> showGameOver(m));
                case EventResolvedMessage m -> Platform.runLater(() -> showResolvedEvents(m));
                case ErrorMessage m -> {
                    LOGGER.fine(() -> "Received error message from server: " + m.getError());
                    guiView.lastErrorMessageAt = System.currentTimeMillis();
                    if (m.getError().equals("CANNOT_AFFORD") || m.getError().equals("INVALID_CARD")) {
                        // Draw rejected by the server: flash the denied feedback on the
                        // exact card the player tried to take, instead of a dialog.
                        Platform.runLater(this::flashLastAttemptedDenied);
                    } else if (m.getError().equals("MUST_PICK_CHARACTER")) {
                        guiView.showError("Errore", "Devi prima prendere il Personaggio disponibile, poi potrai terminare il turno.");
                    } else if (m.getError().equals("SUSPENDED")) {
                        guiView.showInfo("Partita sospesa", m.getErrorDetail());
                    } else if (m.getError().equals("RESUMED")) {
                        guiView.showInfo("Partita ripresa", m.getErrorDetail());
                    } else {
                        guiView.showError("Errore", m.getErrorDetail());
                    }
                }
                case ConnectionUpdateMessage m -> {
                    String who = m.getNickname() == null ? "?" : m.getNickname();
                    LOGGER.fine(() -> who + (m.isConnected() ? " reconnected." : " disconnected."));
                    if (m.isConnected()) {
                        guiView.showInfo("Connessione ripristinata", who + " si è riconnesso.");
                    } else {
                        guiView.showInfo("Giocatore disconnesso", who + " si è disconnesso.");
                    }
                }
                default -> {
                    // Ignore other message types here.
                }
            }
        }
    }

    /**
     * Renders the resolved-events log at the bottom of the left rail (in
     * orange), replacing any previous entries. Events are listed bottom-to-top
     * (first resolved at the bottom), so the latest sits highest. Stays visible
     * until the player places a totem for the next turn (see
     * {@link #clearEventLog()}). Must run on the FX thread.
     */
    private void showResolvedEvents(EventResolvedMessage m) {
        if (eventLogBox == null) return;
        eventLogBox.getChildren().clear();
        List<EventResolvedMessage.ResolvedEvent> events = m.getEvents();
        // Iterate in reverse so the resolution order reads from the bottom up.
        for (int k = events.size() - 1; k >= 0; k--) {
            EventResolvedMessage.ResolvedEvent ev = events.get(k);
            StringBuilder sb = new StringBuilder("• ").append(ev.eventType());
            if (ev.isFinal()) sb.append(" [FINAL]");
            if (ev.deltas().isEmpty()) {
                sb.append(": nessuna variazione");
            } else {
                sb.append(": ");
                for (int i = 0; i < ev.deltas().size(); i++) {
                    EventResolvedMessage.PlayerDelta d = ev.deltas().get(i);
                    if (i > 0) sb.append(", ");
                    sb.append(d.nickname())
                            .append(d.prestigeDelta() >= 0 ? " +" : " ")
                            .append(d.prestigeDelta()).append("PP");
                }
            }
            Label line = new Label(sb.toString());
            line.getStyleClass().add("event-log-entry");
            line.setWrapText(true);
            line.setMaxWidth(Double.MAX_VALUE);
            eventLogBox.getChildren().add(line);
        }
    }

    /** Clears the resolved-events log (called when a totem is placed for the next turn). */
    private void clearEventLog() {
        if (eventLogBox != null) eventLogBox.getChildren().clear();
    }

    /**
     * Adds a card to the upper/lower market row.
     *
     * @param row   row where to add the card
     * @param card  card to add
     * @param index index of the card inside the row
     */
    private void addCardToRow(CardRow row, CardDTO card, int index) {
        Image cardImage = GuiAssets.image(card.front());
        if (cardImage == null) return;

        ImageView cardView = new ImageView(cardImage);
        cardView.fitHeightProperty().bind(cardHeight);
        cardView.setPreserveRatio(true);

        StackPane slot = hoverSlot(cardFrame(cardView));
        slot.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                handleCardClick(row, index);
            } else if (event.getButton() == MouseButton.SECONDARY) {
                displayCardInfo(row, index);
            }
        });
        installCardTip(slot, card, row);

        switch (row) {
            case UPPER -> topCardsRow.getChildren().add(slot);
            case LOWER -> bottomCardsRow.getChildren().add(slot);
        }
    }

    /**
     * Adds an offer tile to the middle row, overlaying the occupant's totem
     * (if any) on top of the tile artwork.
     *
     * @param tile          the offer tile
     * @param occupantColor totem color of the occupying player, or {@code null} if empty
     */
    private void addCardToOfferTrack(OfferTileDTO tile, TotemColor occupantColor) {
        Image tileImage = GuiAssets.image(tile.image());
        if (tileImage == null) return;

        StackPane content = new StackPane();
        content.getStyleClass().add("card-frame");

        ImageView cardView = new ImageView(tileImage);
        cardView.fitHeightProperty().bind(cardHeight);
        cardView.setPreserveRatio(true);
        clipRounded(cardView);
        content.getChildren().add(cardView);

        if (occupantColor != null) {
            ImageView totem = new ImageView(GuiAssets.totemFlat(occupantColor));
            totem.fitHeightProperty().bind(cardHeight.multiply(0.30));
            totem.setPreserveRatio(true);
            StackPane.setAlignment(totem, Pos.TOP_CENTER);
            totem.translateYProperty().bind(cardHeight.multiply(0.18));
            content.getChildren().add(totem);
        }

        StackPane slot = plainSlot(content);
        if (tile.playerIndexOccupant() == guiView.getPlayerId()) {
            content.getChildren().add(cardOutline(cardView));

            Label ownedBadge = new Label("LA TUA TESSERA");
            ownedBadge.getStyleClass().add("offer-owned-badge");
            ownedBadge.setMouseTransparent(true);
            StackPane.setAlignment(ownedBadge, Pos.TOP_CENTER);
            ownedBadge.setTranslateY(-7.0);
            content.getChildren().add(ownedBadge);
        }
        slot.setOnMouseClicked(event -> handleOfferTrackPlacement(tile));
        middleCardsRow.getChildren().add(slot);
    }

    /**
     * Handles player totem placement on the offer track.
     */
    private void handleOfferTrackPlacement(OfferTileDTO tile) {
        if (guiView.isMyTurn()) {
            PlayerAction action = new PlaceTotemAction(guiView.getPlayerId(), tile.id().name());
            View.SendResult result = guiView.sendAndAwaitUpdate(action);
            if (result == View.SendResult.ERROR) {
                LOGGER.fine("Placement rejected by the server rulebook.");
            } else if (result == View.SendResult.OK) {
                // New turn started: drop the previous round's resolved-events log.
                clearEventLog();
            }
        }
    }

    /**
     * Handles clicks on cards in the upper/lower rows.
     *
     * @param row   row of the clicked card
     * @param index index of the clicked card inside the row
     */
    private void handleCardClick(CardRow row, int index) {
        GameDTO g = guiView.getGameView();

        if (!guiView.isMyTurn()) {
            flashDeniedFeedback(row, index);
            return;
        }

        List<CardDTO> targetRowCards = (row == CardRow.UPPER) ? g.upperRowCards() : g.lowerRowCards();
        if (targetRowCards == null || index < 0 || index >= targetRowCards.size()) {
            flashDeniedFeedback(row, index);
            return;
        }

        OfferTileDTO myTile = myOfferTile(g);
        if (myTile == null) {
            flashDeniedFeedback(row, index);
            return;
        }

        // Enforce quota limitations based on current in-flight turn counters.
        if (row == CardRow.UPPER) {
            int maxUpper = View.maxUpperPicks(myTile.id());
            if (turnUpperTaken >= maxUpper) {
                flashDeniedFeedback(row, index);
                return;
            }
        } else {
            int maxLower = View.maxLowerPicks(myTile.id());
            if (turnLowerTaken >= maxLower) {
                flashDeniedFeedback(row, index);
                return;
            }
        }

        if (guiView.getPlayerId() < 0) {
            flashDeniedFeedback(row, index);
            return;
        }

        // Remember the attempted slot so a server-side rejection (e.g. CANNOT_AFFORD,
        // INVALID_CARD) can flash the denied feedback on exactly this card.
        lastAttemptedRow = row;
        lastAttemptedIndex = index;

        View.SendResult result = guiView.sendAndAwaitUpdate(new TakeCardAction(guiView.getPlayerId(), index, row));

        if (result == View.SendResult.OK) {
            if (row == CardRow.UPPER) {
                turnUpperTaken++;
            } else {
                turnLowerTaken++;
            }
        }

        int maxUpper = View.maxUpperPicks(myTile.id());
        int maxLower = View.maxLowerPicks(myTile.id());

        // Automatically return the totem when all possible action quotas are exhausted.
        if (((turnLowerTaken <= maxLower && guiView.getGameView().lowerRowCards().isEmpty()) && turnUpperTaken == maxUpper) ||
                ((turnUpperTaken <= maxUpper && guiView.getGameView().upperRowCards().isEmpty()) && turnLowerTaken == maxLower) ||
                (guiView.getGameView().upperRowCards().isEmpty() && guiView.getGameView().lowerRowCards().isEmpty()) ||
                (turnUpperTaken == maxUpper && turnLowerTaken == maxLower)) {
            endTurnAndReturnTotem();
        }
    }

    /** HUD button: opens the full rules PDF in the system viewer. */
    @FXML
    private void handleShowRules() {
        openRulesPdf();
    }

    /**
     * Extracts the bundled rules PDF ({@code /rules/mesos_rules_it.pdf}) to a
     * temp file and opens it in the system's default viewer. The PDF lives on
     * the classpath so this works from a packaged jar too. Runs the (blocking)
     * extraction and {@code Desktop.open} off the FX thread; failures surface a
     * dialog back on the FX thread.
     */
    private void openRulesPdf() {
        Thread t = new Thread(() -> {
            try {
                java.io.InputStream in = getClass().getResourceAsStream("/rules/mesos_rules_it.pdf");
                if (in == null) {
                    Platform.runLater(() -> guiView.showError("Regole",
                            "PDF delle regole non trovato tra le risorse."));
                    return;
                }
                java.io.File pdf = java.io.File.createTempFile("mesos_rules_", ".pdf");
                pdf.deleteOnExit();
                try (in; java.io.OutputStream out = new java.io.FileOutputStream(pdf)) {
                    in.transferTo(out);
                }
                if (java.awt.Desktop.isDesktopSupported()
                        && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.OPEN)) {
                    java.awt.Desktop.getDesktop().open(pdf);
                } else {
                    Platform.runLater(() -> guiView.showInfo("Regole",
                            "Apertura automatica non supportata su questo sistema.\n"
                                    + "Il PDF è disponibile qui:\n" + pdf.getAbsolutePath()));
                }
            } catch (Exception e) {
                Platform.runLater(() -> guiView.showError("Regole",
                        "Impossibile aprire il PDF delle regole: " + e.getMessage()));
            }
        }, "open-rules-pdf");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Returns the totem and finalizes the player's turn once all required
     * actions are completed or no further legal picks are available.
     */
    private void endTurnAndReturnTotem() {
        GameDTO g = guiView.getGameView();
        if (g == null) {
            guiView.showError("Errore", "Stato inconsistente.");
            return;
        }

        OfferTileDTO myTile = myOfferTile(g);
        if (myTile == null) {
            guiView.showError("Errore", "Impossibile recuperare la tua posizione attuale sulla pista delle offerte.");
            return;
        }

        View.SendResult result = guiView.sendAndAwaitUpdate(new ReturnTotemAction(guiView.getPlayerId()));

        if (result == View.SendResult.OK) {
            turnUpperTaken = 0;
            turnLowerTaken = 0;
            turnFoodTaken = false;
        }
        // On ERROR the dispatcher already surfaced the server reason (e.g.
        // MUST_PICK_CHARACTER); on TIMEOUT sendAndAwaitUpdate already showed a
        // dialog — so no extra generic error here to avoid double pop-ups.
    }

    /**
     * Shows the local player's full tribe (action-bar mini-box click).
     */
    @FXML
    private void handleShowTribePopupDirectly() {
        GameDTO snap = guiView.getGameView();
        if (snap == null) return;
        showTribePopup(snap.players().get(guiView.getPlayerId() - 1), true);
    }

    /**
     * Shows a player's full tribe in a parchment-styled modal, grouped into
     * characters and buildings (matching the redesign). Used both for the local
     * player's own tribe (the action-bar mini-box) and, via the peek arrow on
     * each opponent's player card, for opponents' tribes. Tribes are public
     * information in MESOS (cards lie face-up), so this reveals no hidden state.
     *
     * @param player the player whose tribe to display
     * @param isYou  true for the local player (only changes the title)
     */
    private void showTribePopup(PlayerDTO player, boolean isYou) {
        GameDTO snap = guiView.getGameView();
        if (snap == null || player == null) return;
        List<CardDTO> tribe = player.tribe() == null ? List.of() : player.tribe();
        String windowTitle = isYou ? "La mia tribù" : "Tribù di " + player.nickname();

        VBox root = new VBox(16);
        root.getStyleClass().addAll("parchment-strong");
        root.setPadding(new Insets(22, 24, 24, 24));

        // header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label(windowTitle);
        title.getStyleClass().add("parchment-title");
        Label count = new Label(tribe.size() + " carte");
        count.getStyleClass().add("tribe-count");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label era = new Label("ERA " + roman(snap.currentEra().ordinal() + 1) + " · Round " + snap.currentRound());
        era.getStyleClass().add("tribe-row");
        header.getChildren().addAll(title, count, spacer, era);
        root.getChildren().add(header);

        TribeStats stats = tribeStats(player);
        boolean hideInventorDetails = !isYou && snap.phase() != GamePhase.GAME_OVER;
        root.getChildren().add(tribeSummary(stats, hideInventorDetails));

        // characters section
        List<CardDTO> chars = tribe.stream().filter(c -> c.type() == CardType.CHARACTER).toList();
        if (!chars.isEmpty()) {
            root.getChildren().add(sectionLabel("PERSONAGGI"));
            Map<CharacterType, List<CardDTO>> byType = groupCharactersByType(chars);
            for (CharacterType type : CharacterType.values()) {
                List<CardDTO> group = byType.get(type);
                if (group == null || group.isEmpty()) continue;
                root.getChildren().add(characterGroupHeader(type, group.size(), stats, hideInventorDetails));
                root.getChildren().add(cardFlow(group));
            }
        }

        // buildings section
        List<CardDTO> buildings = tribe.stream().filter(c -> c.type() == CardType.BUILDING).toList();
        if (!buildings.isEmpty()) {
            root.getChildren().add(sectionLabel("EDIFICI · PP stampati +" + stats.printedBuildingPrestige()));
            root.getChildren().add(cardFlow(buildings));
        }
        // empty tribe (common for opponents in the early game)
        if (chars.isEmpty() && buildings.isEmpty()) {
            Label empty = new Label("Nessuna carta nella tribù.");
            empty.getStyleClass().add("tribe-row");
            root.getChildren().add(empty);
        }

        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setPrefSize(720.0, 520.0);
        scrollPane.getStyleClass().add("transparent-scroll");
        scrollPane.setStyle("-fx-background: #EBDCC0; -fx-background-color: #EBDCC0;");

        Stage popupStage = new Stage();
        popupStage.setTitle(windowTitle);
        popupStage.initModality(Modality.APPLICATION_MODAL);
        popupStage.initOwner(middleCardsRow.getScene().getWindow());

        Scene scene = new Scene(scrollPane);
        scene.getStylesheets().addAll(
                getClass().getResource("/javafx_gui/css/theme.css").toExternalForm(),
                getClass().getResource("/javafx_gui/css/game_screen.css").toExternalForm());
        popupStage.setScene(scene);
        popupStage.setResizable(false);
        popupStage.showAndWait();
    }

    /** A small uppercase section label used inside the tribe popup. */
    private Label sectionLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("stat-cap");
        return l;
    }

    /** Compact tribe summary derived from the immutable DTO snapshot. */
    private FlowPane tribeSummary(TribeStats stats, boolean hideInventorDetails) {
        FlowPane summary = new FlowPane();
        summary.setHgap(9);
        summary.setVgap(9);
        summary.getStyleClass().add("tribe-summary");
        summary.getChildren().add(statChip("Personaggi", String.valueOf(stats.characterCount())));
        summary.getChildren().add(statChip("Edifici", String.valueOf(stats.buildingCount())));
        summary.getChildren().add(statChip("Sconto edifici", "-" + stats.builderDiscount() + "F"));
        summary.getChildren().add(statChip("Sconto sostentamento", "-" + stats.collectorDiscount() + "F"));
        summary.getChildren().add(statChip("Icone sciamano", String.valueOf(stats.shamanIcons())));
        summary.getChildren().add(statChip("PP Builder", "+" + stats.builderPrestige()));
        summary.getChildren().add(statChip("PP Artisti", "+" + stats.artistFinalPrestige()));
        summary.getChildren().add(statChip("PP Inventori",
                hideInventorDetails ? "nascosti" : "+" + stats.inventorFinalPrestige()));
        summary.getChildren().add(statChip("PP Edifici", "+" + stats.printedBuildingPrestige()));
        return summary;
    }

    private VBox statChip(String label, String value) {
        Label cap = new Label(label);
        cap.getStyleClass().add("tribe-stat-label");
        Label val = new Label(value);
        val.getStyleClass().add("tribe-stat-value");
        VBox box = new VBox(2, cap, val);
        box.getStyleClass().addAll("stat-card", "tribe-stat");
        return box;
    }

    /** Header for one character role group inside the tribe popup. */
    private HBox characterGroupHeader(CharacterType type, int count, TribeStats stats,
                                      boolean hideInventorDetails) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("tribe-group-header");

        Label title = new Label(characterGroupName(type) + " x" + count);
        title.getStyleClass().add("tribe-group-title");
        Label detail = new Label(characterGroupDetail(type, stats, hideInventorDetails));
        detail.getStyleClass().add("tribe-group-detail");
        detail.setWrapText(true);

        row.getChildren().addAll(title, detail);
        return row;
    }

    private static String characterGroupName(CharacterType type) {
        return switch (type) {
            case BUILDER -> "Costruttori";
            case HUNTER -> "Cacciatori";
            case SHAMAN -> "Sciamani";
            case COLLECTOR -> "Raccoglitori";
            case INVENTOR -> "Inventori";
            case ARTIST -> "Artisti";
        };
    }

    private static String characterGroupDetail(CharacterType type, TribeStats stats,
                                               boolean hideInventorDetails) {
        return switch (type) {
            case BUILDER -> "sconto edifici -" + stats.builderDiscount()
                    + "F · PP finali +" + stats.builderPrestige();
            case HUNTER -> "Evento Caccia: +" + stats.hunterCount()
                    + " cibo e PP in base all'Era";
            case SHAMAN -> "icone totali " + stats.shamanIcons();
            case COLLECTOR -> "sconto Sostentamento -" + stats.collectorDiscount() + "F";
            case INVENTOR -> hideInventorDetails
                    ? "icone invenzione nascoste fino al conteggio finale"
                    : "icone distinte " + stats.distinctInventorIcons()
                    + " · PP finali +" + stats.inventorFinalPrestige();
            case ARTIST -> "coppie " + stats.artistPairs()
                    + " · PP finali +" + stats.artistFinalPrestige();
        };
    }

    /** A wrapping flow of card images. */
    private FlowPane cardFlow(List<CardDTO> cards) {
        FlowPane flow = new FlowPane();
        flow.setHgap(11);
        flow.setVgap(11);
        flow.setPrefWrapLength(660);
        for (CardDTO card : cards) {
            Image img = GuiAssets.image(card.front());
            if (img == null) continue;
            ImageView v = new ImageView(img);
            v.setFitHeight(138);
            v.setPreserveRatio(true);
            v.setSmooth(true);
            flow.getChildren().add(cardFrame(v));
        }
        return flow;
    }

    /**
     * Shows the end-of-game ranking in a styled modal, ordered by prestige.
     */
    private void showGameOver(GameOverMessage m) {
        GameDTO snap = m.getFinalState();
        List<PlayerDTO> ranking = snap.players().stream()
                .sorted((a, b) -> Integer.compare(b.prestigePoints(), a.prestigePoints()))
                .toList();
        playEndGameCinematic(ranking, snap, m.getEndMessage());
    }

    /**
     * Plays the full-screen end-of-game cinematic as an overlay on the game
     * screen (no new Stage). The ranking is already ordered/computed by the
     * model — this is presentation only. Must run on the FX thread.
     *
     * @param ranking    players ordered by prestige (winner first)
     * @param snap       final game snapshot (used for the era kicker)
     * @param endMessage human-readable end summary from the server (may be null)
     */
    private void playEndGameCinematic(List<PlayerDTO> ranking, GameDTO snap, String endMessage) {
        Runnable onNewGame = () -> {
            disconnectQuietly();
            guiView.showStartScreen();
        };
        Runnable onExit = () -> {
            disconnectQuietly();
            Platform.exit();
        };
        new EndGameCinematic(rootPane, ranking, snap.currentEra().ordinal() + 1,
                endMessage, guiView.getPlayerId(), onNewGame, onExit).play();
    }

    /** Best-effort disconnect when leaving a finished match. */
    private void disconnectQuietly() {
        try {
            if (guiView.getConnection() != null) {
                guiView.getConnection().disconnect();
            }
        } catch (Exception ignored) {
            // The match is over; leaving is best effort.
        }
    }

    /**
     * Displays info for a card (right-click on a market card).
     *
     * @param row   card row
     * @param index card index
     */
    void displayCardInfo(CardRow row, int index) {
        CardDTO card = (row == CardRow.UPPER)
                ? guiView.getGameView().upperRowCards().get(index)
                : guiView.getGameView().lowerRowCards().get(index);
        showCardDialog(card, cardTitle(card), cardDescription(card));
    }

    /** Human-readable title for a card, by type/subtype. */
    private static String cardTitle(CardDTO card) {
        return switch (card.type()) {
            case BUILDING -> "Edificio";
            case EVENT -> switch (card.eventType()) {
                case sustenance -> "Evento sostentamento";
                case ritual -> "Evento rituale sciamanico";
                case cavePaintings -> "Evento pitture rupestri";
                case hunt -> "Evento caccia";
            };
            case CHARACTER -> switch (card.characterType()) {
                case BUILDER -> "Costruttore";
                case HUNTER -> "Cacciatore";
                case SHAMAN -> "Sciamano";
                case COLLECTOR -> "Raccoglitore";
                case INVENTOR -> "Inventore";
                case ARTIST -> "Artista";
            };
        };
    }

    /** Rules description for a card, by type/subtype. */
    private String cardDescription(CardDTO card) {
        return switch (card.type()) {
            case EVENT -> switch (card.eventType()) {
                case sustenance -> "Pagate 1 cibo per ogni carta personaggio nella vostra tribù (gli edifici non contano). " +
                        "Se una volta pagato tutto il cibo che avete non siete riusciti a sfamare tutti i " +
                        "vostri personaggi , perdete i punti prestigio indicati dalla carta evento per ogni personaggio che" +
                        " non avete sfamato ";
                case ritual -> "Il giocatore con più icone nella propria tribù, guadagna i punti prestigio indicati." +
                        " Il giocatore con meno icone nella propria tribù, perde i punti prestigio indicati. " +
                        "In caso di parità, tutti i giocatori in parità ottengono o perdono i punti prestigio indicati ";
                case cavePaintings -> "Per ottenere punti con questo evento dovete avere un numero minimo di artisti," +
                        " come indicato sulla carta evento. Se avete il numero di artisti indicato nella riga " +
                        "superiore della carta evento, perdete i punti prestigio indicati. Se invece avete il " +
                        "numero di artisti indicato nella riga inferiore della carta evento guadagnate i punti prestigio " +
                        "indicati per ogni vostro artista ";
                case hunt -> "Prendete un cibo e guadagnate i punti prestigio indicati sulla carta " +
                        "evento per ogni cacciatore nella vostra tribù";
            };
            case CHARACTER -> switch (card.characterType()) {
                case BUILDER -> "Durante la partita ogni costruttore riduce di quanto indicato " +
                        "in alto a destra il costo in cibo di ogni edificio che prendete. A fine partita ogni " +
                        "costruttore fornisce i punti prestigio indicati in basso a sinistra sulla carta ";
                case HUNTER -> "Ogni volta che aggiungete la vostra tribù un cacciatore senza icona, non ottenete niente." +
                        " Ogni volta che aggiungete un cacciatore con un'icona, prendete immediatamente un cibo " +
                        "per ogni cacciatore nella vostra tribù (con o senza icona). Durante l'evento caccia prendete " +
                        "cibo e guadagnate punti prestigio in base al numero di cacciatori nella vostra tribù. ";
                case SHAMAN -> "Possono mostrare da una a tre icone. Durante l'evento rituale sciamanico, avere la" +
                        " maggioranza di queste icone fornisce punti prestigio. Averne invece la minoranza fa perdere punti prestigio.";
                case COLLECTOR -> "Durante l'evento sostentamento forniscono uno sconto di tre cibi sul totale che dovreste pagare." +
                        " Nota: non ottenete mai cibo direttamente dai raccoglitori";
                case INVENTOR -> "A fine partita forniscono un numero di punti prestigio pari al numero di inventori " +
                        "nella vostra tribù moltiplicato per il numero di icone e invenzioni diverse che avete. " +
                        "Ci sono 10 diverse icone invenzione";
                case ARTIST -> "Durante l'evento pittura rupestri potete ottenere o perdere punti prestigio in" +
                        " base al numero di artisti che avete nella vostra tribù. A fine partita ottenete 10 " +
                        "punti prestigio per ogni due pittori nella vostra tribù.";
            };
            case BUILDING -> buildingDescription(card, localPlayer());
        };
    }

    /**
     * Displays a dialog containing a card image and a description.
     *
     * @param card            card DTO
     * @param cardDescription card description
     * @param dialogTitle     title of the dialog
     */
    public void showCardDialog(CardDTO card, String dialogTitle, String cardDescription) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(dialogTitle);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        dialog.getDialogPane().getStylesheets().addAll(
                getClass().getResource("/javafx_gui/css/theme.css").toExternalForm(),
                getClass().getResource("/javafx_gui/css/game_screen.css").toExternalForm());

        Label descriptionLabel = new Label(cardDescription);
        descriptionLabel.setWrapText(true);
        descriptionLabel.setMaxWidth(300);

        VBox dialogContainer = new VBox(15);
        dialogContainer.setAlignment(Pos.CENTER);

        Image cardImage = GuiAssets.image(card.front());
        if (cardImage == null) {
            descriptionLabel.setText("Immagine non trovata! " + cardDescription);
        } else {
            ImageView cardView = new ImageView(cardImage);
            cardView.setFitHeight(150.0);
            cardView.setPreserveRatio(true);
            dialogContainer.getChildren().add(cardFrame(cardView));
        }

        dialogContainer.getChildren().add(descriptionLabel);
        dialog.getDialogPane().setContent(dialogContainer);
        dialog.showAndWait();
    }

    // ------------------------------------------------------------------
    // Visual helpers (background, hover zoom, hover tooltip)
    // ------------------------------------------------------------------

    /**
     * Wraps a card in a fixed-size hover slot. The slot's pickable bounds never
     * change, so the hovered card can enlarge/lift without the cursor leaving it
     * (no flicker). Z-ordering uses {@code viewOrder} so siblings are not
     * reordered/relaid-out (unlike {@code toFront()} inside an HBox).
     */
    private StackPane hoverSlot(Node content) {
        StackPane slot = new StackPane(content);
        slot.setPickOnBounds(true);
        slot.setOnMouseEntered(e -> {
            content.setScaleX(1.04);
            content.setScaleY(1.04);
            content.setTranslateY(0.0);
            slot.setViewOrder(-1);
        });
        slot.setOnMouseExited(e -> {
            content.setScaleX(1.0);
            content.setScaleY(1.0);
            content.setTranslateY(0.0);
            slot.setViewOrder(0);
        });
        return slot;
    }

    /** Fixed slot without hover zoom, used where overlays must stay stable. */
    private StackPane plainSlot(Node content) {
        StackPane slot = new StackPane(content);
        slot.setPickOnBounds(true);
        return slot;
    }

    /** Wraps a card image in the common rounded border frame. */
    private StackPane cardFrame(ImageView cardView) {
        clipRounded(cardView);
        StackPane frame = new StackPane(cardView);
        frame.getStyleClass().add("card-frame");
        return frame;
    }

    /** Clips the image itself so its corners match the rounded frame. */
    private void clipRounded(ImageView imageView) {
        Image image = imageView.getImage();
        double imageHeight = image == null ? 0.0 : image.getHeight();
        double imageWidth = image == null ? 0.0 : image.getWidth();
        double ratio = imageHeight > 0.0 ? imageWidth / imageHeight : 1.0;

        Rectangle clip = new Rectangle();
        clip.setArcWidth(CARD_CORNER_RADIUS);
        clip.setArcHeight(CARD_CORNER_RADIUS);
        clip.widthProperty().bind(imageView.fitHeightProperty().multiply(ratio));
        clip.heightProperty().bind(imageView.fitHeightProperty());
        imageView.setClip(clip);
    }

    /** Orange selection outline that exactly follows a fitHeight-driven card image. */
    private Rectangle cardOutline(ImageView imageView) {
        return cardOutline(imageView, "#E2592B");
    }

    /** Red "draw denied" outline, same geometry as {@link #cardOutline(ImageView)}. */
    private Rectangle cardDeniedOutline(ImageView imageView) {
        return cardOutline(imageView, "#E0432B");
    }

    /** Rounded outline following a fitHeight-driven card image, in the given colour. */
    private Rectangle cardOutline(ImageView imageView, String colorHex) {
        Image image = imageView.getImage();
        double imageHeight = image == null ? 0.0 : image.getHeight();
        double imageWidth = image == null ? 0.0 : image.getWidth();
        double ratio = imageHeight > 0.0 ? imageWidth / imageHeight : 1.0;

        Rectangle outline = new Rectangle();
        outline.setFill(Color.TRANSPARENT);
        outline.setStroke(Color.web(colorHex));
        outline.setStrokeWidth(3.0);
        outline.setArcWidth(CARD_CORNER_RADIUS);
        outline.setArcHeight(CARD_CORNER_RADIUS);
        outline.widthProperty().bind(imageView.fitHeightProperty().multiply(ratio));
        outline.heightProperty().bind(imageView.fitHeightProperty());
        outline.setMouseTransparent(true);
        return outline;
    }

    /**
     * Flashes the "draw denied" feedback on the card last attempted by the
     * player (stored in {@link #lastAttemptedRow}/{@link #lastAttemptedIndex}).
     * Used when the server rejects a draw. Runs on the FX thread.
     */
    private void flashLastAttemptedDenied() {
        if (lastAttemptedRow == null || lastAttemptedIndex < 0) return;
        flashDeniedFeedback(lastAttemptedRow, lastAttemptedIndex);
    }

    /**
     * Transient visual feedback for a rejected draw: a blinking red outline on
     * the clicked card plus a short horizontal shake. Targets only the slot at
     * {@code (row, index)}, auto-clears after ~600ms, and is purely cosmetic
     * (no game state changes). Must run on the FX thread.
     */
    private void flashDeniedFeedback(CardRow row, int index) {
        HBox rowBox = (row == CardRow.UPPER) ? topCardsRow : bottomCardsRow;
        if (rowBox == null || index < 0 || index >= rowBox.getChildren().size()) return;
        if (!(rowBox.getChildren().get(index) instanceof StackPane slot) || slot.getChildren().isEmpty()) return;
        if (!(slot.getChildren().get(0) instanceof StackPane content)) return;

        ImageView cardView = null;
        for (Node n : content.getChildren()) {
            if (n instanceof ImageView iv) { cardView = iv; break; }
        }
        if (cardView == null) return;

        // Drop any leftover denied outline from a previous rapid click.
        content.getChildren().removeIf(n -> "denied-outline".equals(n.getId()));

        Rectangle outline = cardDeniedOutline(cardView);
        outline.setId("denied-outline");
        content.getChildren().add(outline);

        FadeTransition blink = new FadeTransition(Duration.millis(150), outline);
        blink.setFromValue(1.0);
        blink.setToValue(0.15);
        blink.setAutoReverse(true);
        blink.setCycleCount(4);                 // ~600ms total
        blink.setOnFinished(e -> content.getChildren().remove(outline));

        TranslateTransition shake = new TranslateTransition(Duration.millis(55), content);
        shake.setFromX(0);
        shake.setByX(6);
        shake.setAutoReverse(true);
        shake.setCycleCount(6);
        shake.setOnFinished(e -> content.setTranslateX(0));

        blink.play();
        shake.play();
    }

    /** Clips images whose size is driven by fitWidth instead of fitHeight. */
    private void clipRoundedToFitWidth(ImageView imageView) {
        Image image = imageView.getImage();
        double imageHeight = image == null ? 0.0 : image.getHeight();
        double imageWidth = image == null ? 0.0 : image.getWidth();
        double ratio = imageWidth > 0.0 ? imageHeight / imageWidth : 1.0;

        Rectangle clip = new Rectangle();
        clip.setArcWidth(CARD_CORNER_RADIUS);
        clip.setArcHeight(CARD_CORNER_RADIUS);
        clip.widthProperty().bind(imageView.fitWidthProperty());
        clip.heightProperty().bind(imageView.fitWidthProperty().multiply(ratio));
        imageView.setClip(clip);
    }

    /** Installs a rich, themed description popup on a card node. */
    private void installCardTip(Node node, CardDTO card, CardRow row) {
        Popup popup = new Popup();
        popup.setAutoFix(true);
        popup.setAutoHide(false);
        popup.setHideOnEscape(true);

        VBox tip = buildCardTip(card);
        tip.getStylesheets().addAll(
                getClass().getResource("/javafx_gui/css/theme.css").toExternalForm(),
                getClass().getResource("/javafx_gui/css/game_screen.css").toExternalForm());
        popup.getContent().add(tip);

        node.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> showCardTip(node, popup, row));
        node.addEventHandler(MouseEvent.MOUSE_MOVED, e -> {
            if (!popup.isShowing()) {
                showCardTip(node, popup, row);
            }
        });
        node.addEventHandler(MouseEvent.MOUSE_EXITED, e -> popup.hide());
    }

    private void showCardTip(Node node, Popup popup, CardRow row) {
        if (node.getScene() == null || node.getScene().getWindow() == null) return;
        Bounds bounds = node.localToScreen(node.getBoundsInLocal());
        if (bounds == null) return;
        if (popup.isShowing()) return;

        Node content = popup.getContent().get(0);
        content.applyCss();
        content.autosize();
        double popupWidth = Math.max(content.prefWidth(-1), content.getLayoutBounds().getWidth());
        double popupHeight = Math.max(content.prefHeight(popupWidth), content.getLayoutBounds().getHeight());
        double x = bounds.getMinX() + (bounds.getWidth() - popupWidth) / 2.0;
        double y = row == CardRow.UPPER
                ? bounds.getMaxY() + 10.0
                : bounds.getMinY() - popupHeight - 10.0;
        popup.show(node, x, y);
    }

    /** Builds the dark popup body (title, chips, description) for a card. */
    private VBox buildCardTip(CardDTO card) {
        VBox box = new VBox(8);
        box.getStyleClass().add("card-tip");
        box.setMaxWidth(232);

        Label title = new Label(cardTitle(card));
        title.getStyleClass().add("card-tip-title");
        box.getChildren().add(title);

        HBox chips = new HBox(6);
        chips.setAlignment(Pos.CENTER_LEFT);
        switch (card.type()) {
            case CHARACTER -> chips.getChildren().add(chip("Personaggio", "card-chip-type"));
            case EVENT -> chips.getChildren().add(chip("Evento", "card-chip-type"));
            case BUILDING -> {
                chips.getChildren().add(chip("Edificio", "card-chip-type"));
                if (card.buildingFoodCost() != null) {
                    chips.getChildren().add(chip(card.buildingFoodCost() + " cibo", "card-chip-food"));
                }
                PlayerDTO localPlayer = localPlayer();
                if (localPlayer != null && builderDiscount(localPlayer) > 0) {
                    chips.getChildren().add(chip("paghi "
                            + effectiveBuildingCost(card, localPlayer) + " cibo", "card-chip-food"));
                }
                if (card.buildingPrestigeBonus() != null) {
                    chips.getChildren().add(chip(card.buildingPrestigeBonus() + " PP", "card-chip-pp"));
                }
            }
        }
        if (!chips.getChildren().isEmpty()) box.getChildren().add(chips);

        Label desc = new Label(cardDescription(card));
        desc.getStyleClass().add("card-tip-desc");
        desc.setWrapText(true);
        desc.setMaxWidth(210);
        box.getChildren().add(desc);
        return box;
    }

    /** A small rounded tag used inside the hover tooltip. */
    private Label chip(String text, String modifierClass) {
        Label l = new Label(text);
        l.getStyleClass().addAll("card-chip", modifierClass);
        return l;
    }

    // ------------------------------------------------------------------
    // Small helpers
    // ------------------------------------------------------------------

    private String buildingDescription(CardDTO card, PlayerDTO viewer) {
        int printedCost = card.buildingFoodCost() == null ? 0 : card.buildingFoodCost();
        int prestige = card.buildingPrestigeBonus() == null ? 0 : card.buildingPrestigeBonus();
        if (viewer == null) {
            return "Questa carta edificio ha un costo di " + printedCost + " cibo e "
                    + "permette di ottenere " + prestige + " punti prestigio a fine partita. "
                    + "L'effetto è riportato nell'immagine.";
        }

        int discount = builderDiscount(viewer);
        int effectiveCost = effectiveBuildingCost(card, viewer);
        String affordability = effectiveCost <= viewer.foodPoints()
                ? "Puoi acquistarla con il cibo attuale."
                : "Non hai abbastanza cibo per acquistarla ora.";
        return "Costo stampato: " + printedCost + " cibo. "
                + "Sconto Costruttori: -" + discount + " cibo. "
                + "Paghi: " + effectiveCost + " cibo. "
                + affordability + " "
                + "A fine partita vale " + prestige + " PP stampati. "
                + "L'effetto è riportato nell'immagine.";
    }

    private boolean hasPickableCard(List<CardDTO> cards, PlayerDTO player) {
        if (cards == null) return false;
        for (CardDTO card : cards) {
            if (card == null) continue;
            if (card.type() == CardType.CHARACTER) return true;
            if (card.type() == CardType.BUILDING && player != null
                    && effectiveBuildingCost(card, player) <= player.foodPoints()) {
                return true;
            }
        }
        return false;
    }

    private PlayerDTO localPlayer() {
        return playerById(guiView.getGameView(), guiView.getPlayerId());
    }

    private static PlayerDTO playerById(GameDTO snap, int playerId) {
        if (snap == null || snap.players() == null) return null;
        for (PlayerDTO player : snap.players()) {
            if (player != null && player.playerId() == playerId) {
                return player;
            }
        }
        return null;
    }

    private static int effectiveBuildingCost(CardDTO building, PlayerDTO player) {
        int printedCost = building == null || building.buildingFoodCost() == null
                ? 0
                : building.buildingFoodCost();
        return Math.max(0, printedCost - builderDiscount(player));
    }

    private static int builderDiscount(PlayerDTO player) {
        return tribeStats(player).builderDiscount();
    }

    private static Map<CharacterType, List<CardDTO>> groupCharactersByType(List<CardDTO> cards) {
        EnumMap<CharacterType, List<CardDTO>> byType = new EnumMap<>(CharacterType.class);
        for (CharacterType type : CharacterType.values()) {
            byType.put(type, new ArrayList<>());
        }
        if (cards == null) return byType;
        for (CardDTO card : cards) {
            if (card != null && card.type() == CardType.CHARACTER && card.characterType() != null) {
                byType.get(card.characterType()).add(card);
            }
        }
        return byType;
    }

    private static TribeStats tribeStats(PlayerDTO player) {
        return tribeStats(player == null ? null : player.tribe());
    }

    private static TribeStats tribeStats(List<CardDTO> tribe) {
        int characterCount = 0;
        int buildingCount = 0;
        int builderDiscount = 0;
        int collectorDiscount = 0;
        int builderPrestige = 0;
        int hunterCount = 0;
        int shamanIcons = 0;
        int inventorCount = 0;
        int artistCount = 0;
        int printedBuildingPrestige = 0;
        Set<Integer> inventorIcons = new HashSet<>();

        if (tribe != null) {
            for (CardDTO card : tribe) {
                if (card == null || card.type() == null) continue;
                if (card.type() == CardType.BUILDING) {
                    buildingCount++;
                    printedBuildingPrestige += card.buildingPrestigeBonus() == null
                            ? 0
                            : card.buildingPrestigeBonus();
                    continue;
                }
                if (card.type() != CardType.CHARACTER || card.characterType() == null) continue;

                characterCount++;
                switch (card.characterType()) {
                    case BUILDER -> {
                        builderDiscount += card.builderFoodBonus() == null ? 0 : card.builderFoodBonus();
                        builderPrestige += card.builderPrestigePoints() == null ? 0 : card.builderPrestigePoints();
                    }
                    case HUNTER -> hunterCount++;
                    case SHAMAN -> shamanIcons += card.shamanIcons() == null ? 0 : card.shamanIcons();
                    case COLLECTOR -> collectorDiscount += card.collectorFoodDiscount() == null
                            ? 0
                            : card.collectorFoodDiscount();
                    case INVENTOR -> {
                        inventorCount++;
                        if (card.inventorIcon() != null) {
                            inventorIcons.add(card.inventorIcon());
                        }
                    }
                    case ARTIST -> artistCount++;
                }
            }
        }

        return new TribeStats(characterCount, buildingCount, builderDiscount, collectorDiscount,
                builderPrestige, hunterCount, shamanIcons, inventorCount, inventorIcons.size(),
                artistCount, printedBuildingPrestige);
    }

    private record TribeStats(int characterCount, int buildingCount, int builderDiscount,
                              int collectorDiscount, int builderPrestige, int hunterCount,
                              int shamanIcons, int inventorCount, int distinctInventorIcons,
                              int artistCount, int printedBuildingPrestige) {
        int artistPairs() {
            return artistCount / 2;
        }

        int artistFinalPrestige() {
            return artistPairs() * 10;
        }

        int inventorFinalPrestige() {
            return inventorCount * distinctInventorIcons;
        }
    }

    /** Returns this player's occupied offer tile, or {@code null}. */
    private OfferTileDTO myOfferTile(GameDTO g) {
        if (g == null) return null;
        return g.offerTrack().stream()
                .filter(tile -> tile.playerIndexOccupant() == guiView.getPlayerId())
                .findFirst()
                .orElse(null);
    }

    /** Replaces the single layout style class on a node (pill ⇄ pill-accent). */
    private static void setStyleClass(HBox node, String styleClass) {
        node.getStyleClass().removeAll("pill", "pill-accent");
        node.getStyleClass().add(styleClass);
    }

    /** Tiny roman-numeral helper for era display (1..3 expected). */
    private static String roman(int n) {
        return switch (n) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            default -> String.valueOf(n);
        };
    }
}
