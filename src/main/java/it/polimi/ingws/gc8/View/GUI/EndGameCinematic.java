package it.polimi.ingws.gc8.View.GUI;

import it.polimi.ingws.gc8.Model.Enum.CardType;
import it.polimi.ingws.gc8.Network.DTO.CardDTO;
import it.polimi.ingws.gc8.Network.DTO.PlayerDTO;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.effect.InnerShadow;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

/**
 * Full-screen cinematic shown when the game ends ({@code GameOverMessage}).
 *
 * <p>Pure presentation: it receives an already ordered/computed leaderboard
 * ({@link PlayerDTO} list sorted by prestige) and never touches scoring logic.
 * It installs itself as the last child of the host {@code StackPane} (the game
 * screen root), so it overlays everything without opening a new {@code Stage}.</p>
 *
 * <p>Lifecycle (all on the FX thread):</p>
 * <ol>
 *   <li><b>Phase A</b> ({@link #buildOpeningPane()}): backdrop darkens, embers
 *       rise, a parchment unrolls top-to-bottom revealing "PARTITA TERMINATA"
 *       and a "Scopri chi ha vinto" button.</li>
 *   <li><b>Phase B</b> ({@link #buildLeaderboardPane()}): the parchment rolls
 *       back up and a larger one unrolls with the final ranking composing row by
 *       row, plus "Nuova partita" / "Esci".</li>
 * </ol>
 *
 * <p>Re-playable: every call builds a brand-new overlay; no animated instance is
 * ever reused. A pre-existing overlay (e.g. a duplicate message) is removed
 * first.</p>
 */
public final class EndGameCinematic {

    private static final Logger LOGGER = Logger.getLogger(EndGameCinematic.class.getName());

    /** Marks the overlay node so a previous instance can be cleared on replay. */
    private static final String OVERLAY_ID = "end-game-cinematic-overlay";

    // Theme palette (reused from the rest of the GUI).
    private static final Color ORANGE = Color.web("#E2592B");
    private static final Color GOLD = Color.web("#EAB14A");
    private static final Color IVORY_TOP = Color.web("#F4EAD3");
    private static final Color IVORY_BOTTOM = Color.web("#E7D6B8");
    private static final Color INK = Color.web("#3A2A18");
    private static final Color INK_SOFT = Color.web("#8A6D3F");
    private static final Color[] EMBER_COLORS = {
            Color.web("#F0A05A"), Color.web("#EAB14A"), Color.web("#E2592B")
    };

    private final StackPane host;
    private final List<PlayerDTO> ranking;
    private final int eraNumber;
    private final String endMessage;
    private final int localPlayerId;
    private final Runnable onNewGame;
    private final Runnable onExit;

    private StackPane overlay;
    private StackPane centerLayer;

    /**
     * @param host          the game-screen root {@code StackPane} the overlay is added to
     * @param ranking       players already ordered by prestige (winner first)
     * @param eraNumber     era reached (1-3), shown in the opening kicker
     * @param endMessage    human-readable end summary from the server (may be null)
     * @param localPlayerId id of the local player, to label their own row as "Tu"
     * @param onNewGame     action for the "Nuova partita" button
     * @param onExit        action for the "Esci" button
     */
    public EndGameCinematic(StackPane host, List<PlayerDTO> ranking, int eraNumber,
                            String endMessage, int localPlayerId,
                            Runnable onNewGame, Runnable onExit) {
        this.host = host;
        this.ranking = ranking;
        this.eraNumber = eraNumber;
        this.endMessage = endMessage;
        this.localPlayerId = localPlayerId;
        this.onNewGame = onNewGame;
        this.onExit = onExit;
    }

    /** Builds the overlay and starts Phase A. Must be called on the FX thread. */
    public void play() {
        host.getChildren().removeIf(n -> OVERLAY_ID.equals(n.getId()));

        overlay = new StackPane();
        overlay.setId(OVERLAY_ID);
        overlay.prefWidthProperty().bind(host.widthProperty());
        overlay.prefHeightProperty().bind(host.heightProperty());

        Region backdrop = buildBackdrop();
        Pane embers = buildEmbers();
        centerLayer = new StackPane();
        centerLayer.setPickOnBounds(false);

        overlay.getChildren().addAll(backdrop, embers, centerLayer);
        // Swallow any click that lands on the dimmed background.
        overlay.setOnMouseClicked(e -> e.consume());
        host.getChildren().add(overlay);

        FadeTransition appear = new FadeTransition(Duration.millis(600), overlay);
        appear.setFromValue(0);
        appear.setToValue(1);
        appear.play();

        centerLayer.getChildren().setAll(buildOpeningPane());
        LOGGER.fine("End-game cinematic started.");
    }

    // ---------------------------------------------------------------- backdrop

    private Region buildBackdrop() {
        Region backdrop = new Region();
        LinearGradient g = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#0B0907", 0.94)),
                new Stop(0.68, Color.web("#1A0F08", 0.95)),
                new Stop(1, Color.web("#E2592B", 0.30)));
        backdrop.setBackground(new Background(new BackgroundFill(g, CornerRadii.EMPTY, Insets.EMPTY)));
        return backdrop;
    }

    private Pane buildEmbers() {
        Pane layer = new Pane();
        layer.setMouseTransparent(true);
        for (int i = 0; i < 10; i++) {
            double radius = 2 + ThreadLocalRandom.current().nextDouble() * 1.4;
            Color color = EMBER_COLORS[i % EMBER_COLORS.length];
            Circle ember = new Circle(radius, color);

            DropShadow halo = new DropShadow(10, color);
            halo.setInput(new Glow(0.8));
            ember.setEffect(halo);

            double frac = (i + 0.5) / 10.0 + (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.06;
            ember.layoutXProperty().bind(layer.widthProperty().multiply(frac));
            ember.layoutYProperty().bind(layer.heightProperty().add(12));

            double rise = 240 + ThreadLocalRandom.current().nextDouble() * 90;
            double seconds = 4 + ThreadLocalRandom.current().nextDouble() * 1.5;
            double delay = ThreadLocalRandom.current().nextDouble() * seconds;

            TranslateTransition up = new TranslateTransition(Duration.seconds(seconds), ember);
            up.setFromY(0);
            up.setToY(-rise);
            up.setInterpolator(Interpolator.EASE_OUT);
            up.setCycleCount(TranslateTransition.INDEFINITE);
            up.setDelay(Duration.seconds(delay));

            FadeTransition flicker = new FadeTransition(Duration.seconds(seconds), ember);
            flicker.setFromValue(0.85);
            flicker.setToValue(0.0);
            flicker.setCycleCount(FadeTransition.INDEFINITE);
            flicker.setDelay(Duration.seconds(delay));

            layer.getChildren().add(ember);
            up.play();
            flicker.play();
        }
        return layer;
    }

    // ------------------------------------------------------------- Phase A

    private Pane buildOpeningPane() {
        double w = 520;
        double h = 360;

        Label kicker = parchmentLabel("ERA " + roman(eraNumber) + " COMPLETATA", 15, FontWeight.SEMI_BOLD, INK_SOFT);
        kicker.setStyle("-fx-letter-spacing: 3px;");
        Label title = parchmentLabel("PARTITA TERMINATA", 40, FontWeight.EXTRA_BOLD, INK);
        // Guard against the title overflowing the parchment (and being clipped to
        // "PARTITA TERMIN…") when the System font renders wider on some screens.
        title.setWrapText(true);
        title.setMaxWidth(w - 72);
        title.setAlignment(Pos.CENTER);
        title.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        Button reveal = new Button("Scopri chi ha vinto ›");
        reveal.getStyleClass().add("game-over-primary");
        reveal.setOnAction(e -> goToLeaderboard());

        // No subtitle here on purpose: it used to show the server end message,
        // which already named the winner — spoiling the "Scopri chi ha vinto"
        // reveal. The winner is shown only on the leaderboard (Phase B).
        VBox content = new VBox(16, kicker, title, reveal);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(40, 36, 40, 36));

        for (Node n : List.of(kicker, title, reveal)) {
            n.setOpacity(0);
        }

        Scroll scroll = makeScroll(w, h, content);
        unroll(scroll, 550, () -> {
            cascadeIn(kicker, 0, 16);
            cascadeIn(title, 200, 16);
            cascadeIn(reveal, 500, 16);
        });
        return scroll.layer;
    }

    private void goToLeaderboard() {
        Pane openingLayer = (Pane) centerLayer.getChildren().get(0);
        Scroll opening = (Scroll) openingLayer.getUserData();

        FadeTransition fadeText = new FadeTransition(Duration.millis(350), opening.parchment);
        fadeText.setFromValue(1);
        fadeText.setToValue(0.0);

        Timeline rollUp = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(opening.clip.heightProperty(), opening.clip.getHeight())),
                new KeyFrame(Duration.millis(700), new KeyValue(opening.clip.heightProperty(), 0,
                        Interpolator.SPLINE(0.4, 0, 0.7, 0.2))));

        ParallelTransition close = new ParallelTransition(fadeText, rollUp);
        close.setOnFinished(e -> centerLayer.getChildren().setAll(buildLeaderboardPane()));
        close.play();
    }

    // ------------------------------------------------------------- Phase B

    private Pane buildLeaderboardPane() {
        double w = 620;
        // Grow with the player count (2-5) so no row is clipped by the parchment.
        double h = 210 + ranking.size() * 70;

        Label header = parchmentLabel("CLASSIFICA FINALE", 30, FontWeight.EXTRA_BOLD, INK);

        VBox rows = new VBox(10);
        rows.setAlignment(Pos.TOP_CENTER);
        rows.setFillWidth(true);
        for (int i = 0; i < ranking.size(); i++) {
            HBox row = buildRankingRow(i + 1, ranking.get(i), i == 0);
            row.setOpacity(0);
            row.setTranslateY(-12);
            rows.getChildren().add(row);
        }

        Button newGame = new Button("Nuova partita");
        newGame.getStyleClass().add("game-over-primary");
        newGame.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(newGame, Priority.ALWAYS);
        newGame.setOnAction(e -> dismiss(onNewGame));

        Button exit = new Button("Esci");
        exit.getStyleClass().add("game-over-secondary");
        // Inline style wins over the .game-over-secondary author stylesheet,
        // which would otherwise keep the text light grey (setTextFill is lower
        // priority than an author stylesheet in JavaFX CSS).
        exit.setStyle("-fx-text-fill: black;");
        exit.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(exit, Priority.ALWAYS);
        exit.setOnAction(e -> dismiss(onExit));

        HBox actions = new HBox(16, newGame, exit);
        actions.setAlignment(Pos.CENTER);
        actions.setOpacity(0);

        VBox content = new VBox(18, header, rows, actions);
        content.setAlignment(Pos.TOP_CENTER);
        content.setPadding(new Insets(30, 34, 28, 34));
        VBox.setVgrow(rows, Priority.ALWAYS);

        Scroll scroll = makeScroll(w, h, content);
        unroll(scroll, 150, () -> {
            for (int i = 0; i < rows.getChildren().size(); i++) {
                cascadeIn(rows.getChildren().get(i), i * 120L, -12);
            }
            cascadeIn(actions, rows.getChildren().size() * 120L + 150L, 10);
        });
        return scroll.layer;
    }

    private HBox buildRankingRow(int rank, PlayerDTO p, boolean winner) {
        HBox row = new HBox(18);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 16, 10, 14));
        Color rowFill = winner ? Color.web("#EAB14A", 0.28) : Color.web("#3A2A18", 0.06);
        row.setBackground(new Background(new BackgroundFill(rowFill, new CornerRadii(12), Insets.EMPTY)));

        Label rankLabel = parchmentLabel(String.valueOf(rank), 22, FontWeight.EXTRA_BOLD, winner ? GOLD.darker() : INK_SOFT);
        rankLabel.setMinWidth(28);

        ImageView totem = new ImageView(GuiAssets.totem3D(p.totemColor()));
        totem.setFitHeight(46);
        totem.setPreserveRatio(true);

        Label name = parchmentLabel(p.playerId() == localPlayerId ? "Tu" : p.nickname(), 19, FontWeight.BOLD, INK);
        HBox nameLine = new HBox(10, name);
        nameLine.setAlignment(Pos.CENTER_LEFT);
        if (winner) {
            Label badge = new Label("VINCITORE");
            badge.setFont(Font.font("System", FontWeight.BOLD, 11));
            badge.setTextFill(INK);
            badge.setPadding(new Insets(2, 8, 2, 8));
            badge.setBackground(new Background(new BackgroundFill(GOLD, new CornerRadii(8), Insets.EMPTY)));
            nameLine.getChildren().add(badge);
        }
        Label stats = parchmentLabel(statsLine(p), 13, FontWeight.NORMAL, INK_SOFT);
        VBox info = new VBox(3, nameLine, stats);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label score = parchmentLabel(String.valueOf(p.prestigePoints()), 26, FontWeight.EXTRA_BOLD, winner ? GOLD.darker() : INK);
        Label pp = parchmentLabel("PP", 13, FontWeight.SEMI_BOLD, INK_SOFT);
        HBox scoreBox = new HBox(7, score, pp);
        scoreBox.setAlignment(Pos.BASELINE_RIGHT);

        row.getChildren().addAll(rankLabel, totem, info, scoreBox);

        if (winner) {
            Glow glow = new Glow(0.25);
            row.setEffect(glow);
            Timeline pulse = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(glow.levelProperty(), 0.2)),
                    new KeyFrame(Duration.seconds(1.1), new KeyValue(glow.levelProperty(), 0.8)));
            pulse.setAutoReverse(true);
            pulse.setCycleCount(Timeline.INDEFINITE);
            pulse.play();
        }
        return row;
    }

    private String statsLine(PlayerDTO p) {
        long buildings = p.tribe().stream().filter(c -> c.type() == CardType.BUILDING).count();
        long events = p.tribe().stream().filter(c -> c.type() == CardType.EVENT).count();
        long characters = p.tribe().stream().filter(c -> c.type() == CardType.CHARACTER).count();
        return "tribù " + characters + " · edifici " + buildings + " · eventi " + events;
    }

    // ------------------------------------------------------------- scroll/unroll

    /** A parchment with its unroll clip and follower rod, ready to be animated. */
    private static final class Scroll {
        final Pane layer;
        final StackPane parchment;
        final Rectangle clip;

        Scroll(Pane layer, StackPane parchment, Rectangle clip) {
            this.layer = layer;
            this.parchment = parchment;
            this.clip = clip;
        }
    }

    /**
     * Builds an ivory parchment of {@code w}x{@code h} wrapping {@code content},
     * clipped to height 0 (rolled up) with a wooden rod tracking the reveal edge.
     */
    private Scroll makeScroll(double w, double h, Node content) {
        double pad = 14;
        Pane layer = new Pane();
        layer.setMinSize(w + pad * 2, h + pad * 2);
        layer.setPrefSize(w + pad * 2, h + pad * 2);
        layer.setMaxSize(w + pad * 2, h + pad * 2);
        layer.setPickOnBounds(false);

        StackPane parchment = new StackPane(content);
        parchment.setLayoutX(pad);
        parchment.setLayoutY(pad);
        parchment.setMinSize(w, h);
        parchment.setPrefSize(w, h);
        parchment.setMaxSize(w, h);
        LinearGradient ivory = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, IVORY_TOP), new Stop(1, IVORY_BOTTOM));
        parchment.setBackground(new Background(new BackgroundFill(ivory, new CornerRadii(16), Insets.EMPTY)));
        InnerShadow inner = new InnerShadow(22, Color.web("#9C7B4E", 0.5));
        parchment.setEffect(inner);

        Rectangle clip = new Rectangle(w, 0);
        clip.setArcWidth(28);
        clip.setArcHeight(28);
        parchment.setClip(clip);

        Region rod = new Region();
        double rodH = 16;
        rod.setMinSize(w + 24, rodH);
        rod.setPrefSize(w + 24, rodH);
        rod.setMaxSize(w + 24, rodH);
        rod.setLayoutX(pad - 12);
        LinearGradient wood = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#7A5530")), new Stop(0.5, Color.web("#A9763F")),
                new Stop(1, Color.web("#5C3D20")));
        rod.setBackground(new Background(new BackgroundFill(wood, new CornerRadii(8), Insets.EMPTY)));
        rod.setEffect(new DropShadow(10, Color.web("#000000", 0.45)));
        // Rod center follows the clip's bottom edge (parchment is offset by `pad`).
        rod.translateYProperty().bind(clip.heightProperty().add(pad - rodH / 2));

        layer.getChildren().addAll(parchment, rod);

        Scroll scroll = new Scroll(layer, parchment, clip);
        layer.setUserData(scroll);
        return scroll;
    }

    /** Animates the parchment clip from 0 to full height after {@code delayMs}. */
    private void unroll(Scroll scroll, long delayMs, Runnable afterUnroll) {
        double full = scroll.parchment.getPrefHeight();
        Timeline roll = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(scroll.clip.heightProperty(), 0)),
                new KeyFrame(Duration.millis(1000), new KeyValue(scroll.clip.heightProperty(), full,
                        Interpolator.SPLINE(0.25, 0.9, 0.3, 1))));
        roll.setDelay(Duration.millis(delayMs));
        if (afterUnroll != null) {
            roll.setOnFinished(e -> afterUnroll.run());
        }
        roll.play();
    }

    /** Fade + slide a node into place after {@code delayMs}, starting offset by {@code fromY}. */
    private void cascadeIn(Node node, long delayMs, double fromY) {
        node.setOpacity(0);
        node.setTranslateY(fromY);
        FadeTransition fade = new FadeTransition(Duration.millis(480), node);
        fade.setFromValue(0);
        fade.setToValue(1);
        TranslateTransition slide = new TranslateTransition(Duration.millis(480), node);
        slide.setFromY(fromY);
        slide.setToY(0);
        slide.setInterpolator(Interpolator.EASE_OUT);
        ParallelTransition in = new ParallelTransition(fade, slide);
        in.setDelay(Duration.millis(delayMs));
        in.play();
    }

    /** Fades the whole overlay out, removes it, then runs {@code action}. */
    private void dismiss(Runnable action) {
        FadeTransition out = new FadeTransition(Duration.millis(350), overlay);
        out.setFromValue(overlay.getOpacity());
        out.setToValue(0);
        out.setOnFinished(e -> {
            host.getChildren().remove(overlay);
            if (action != null) {
                action.run();
            }
        });
        out.play();
    }

    // ------------------------------------------------------------- helpers

    private Label parchmentLabel(String text, double size, FontWeight weight, Color color) {
        Label label = new Label(text);
        label.setFont(Font.font("System", weight, size));
        label.setTextFill(color);
        return label;
    }

    private static String roman(int n) {
        return switch (n) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            default -> String.valueOf(n);
        };
    }
}
