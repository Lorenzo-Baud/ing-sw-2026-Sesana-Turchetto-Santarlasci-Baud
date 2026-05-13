package it.polimi.ingws.gc8.Client;

import it.polimi.ingws.gc8.Controller.MainController;
import it.polimi.ingws.gc8.Network.Connection.RMI.RMIServerConnection;
import it.polimi.ingws.gc8.Network.Connection.ServerConnection;
import it.polimi.ingws.gc8.Network.Connection.Socket.SocketServerConnection;
import it.polimi.ingws.gc8.Network.DTO.LoginResult;
import it.polimi.ingws.gc8.View.GUI.GuiView;
import it.polimi.ingws.gc8.View.TUI.Printer;
import it.polimi.ingws.gc8.View.TUI.TuiView;
import it.polimi.ingws.gc8.View.View;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;

/**
 * Client-side entry point.
 *
 * <p>Wiring (top-down):</p>
 * <ol>
 *   <li>{@link Bootstrap} collects UI flavor, host, port and nickname.</li>
 *   <li>Build a {@link ServerConnection} for the chosen transport (currently
 *       only RMI; socket pluggable in {@link #createConnection}).</li>
 *   <li>Build the concrete {@link View} for the chosen UI flavor.</li>
 *   <li>Build a {@link ClientController} bridging View ↔ network.</li>
 *   <li>{@code connect()} → {@code controller.start()} → {@code login()}.</li>
 *   <li>If the server reports {@code isFirst==true}, ask the host how many
 *       players the match must have and call {@code setExpectedPlayers()}.</li>
 *   <li>{@code view.run()} (blocking): the user sees the lobby progression
 *       and the game starts automatically when all players have joined.</li>
 * </ol>
 */
public class ClientMain {

    public static void main(String[] args) {
        BootstrapConfig cfg;
        try {
            cfg = Bootstrap.run();
        } catch (Exception e) {
            System.err.println("[Client] Bootstrap failed: " + e.getMessage());
            return;
        }


        ServerConnection connection = createConnection(cfg.transport() );
        View view = createView(cfg.uiFlavor());
        ClientController controller = new ClientController(connection, view);

        // Graceful shutdown on Ctrl+C / JVM exit.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            controller.stop();
            connection.disconnect();
        }, "client-shutdown"));

        try {
            try {
                connection.connect(cfg.host(), cfg.port());
            } catch (Exception ce) {
                Printer.printError(friendlyConnectError(ce, cfg.host(), cfg.port()));
                return;
            }
            Printer.printSuccess("[Client] Connected to "
                    + cfg.host() + ":" + cfg.port());

            // Start the network pump BEFORE login: it must be active when
            // the server starts pushing lobby callbacks to us.
            controller.start();

            view.setNickname(cfg.nickname());
            LoginResult login = connection.login(cfg.nickname());
            view.setPlayerId(login.playerId());
            Printer.printSuccess("[Client] Logged in as '" + cfg.nickname()
                    + "' (playerId=" + login.playerId()
                    + (login.isFirst() ? ", host" : "") + ")");

            if (login.isFirst()) {
                int n = Bootstrap.askExpectedPlayers(
                        MainController.MIN_PLAYERS, MainController.MAX_PLAYERS);
                connection.setExpectedPlayers(n);
                Printer.printInfo("Lobby configurata per " + n + " giocatori. "
                        + "In attesa degli altri…");
            } else {
                Printer.printInfo("Sei in lobby, in attesa che la partita parta…");
            }

            // Hands off to the View: blocks until the user quits or the
            // game ends. The View's dispatcher will print LobbyUpdate
            // messages and proceed to the game once the server auto-starts.
            view.run();

        } catch (Exception e) {
            Printer.printError("[Client] " + e.getMessage());
            e.printStackTrace();
        } finally {
            controller.stop();
            connection.disconnect();
        }
    }

    private static View createView(UiFlavor flavor) {
        return switch (flavor) {
            case TUI -> new TuiView();
            case GUI -> new GuiView();
        };
    }

    private static ServerConnection createConnection(String transport) {
        return switch (transport) {
            case "rmi" -> new RMIServerConnection();
            case "socket" -> new SocketServerConnection();   // future
            default -> throw new IllegalArgumentException("Unknown transport: " + transport);
        };
    }

    /**
     * Translates the raw RMI/network exception thrown by
     * {@link ServerConnection#connect(String, int)} into a user-friendly
     * sentence shown via {@link Printer#printError(String)} — never a
     * stack trace.
     */
    private static String friendlyConnectError(Throwable e, String host, int port) {
        Throwable cur = e;
        while (cur != null) {
            if (cur instanceof UnknownHostException) {
                return "Hostname '" + host + "' non risolvibile. "
                        + "Inserire un IP o un nome DNS valido.";
            }
            if (cur instanceof ConnectException) {
                return "Il server non è raggiungibile su " + host + ":" + port + ". "
                        + "Verificare che sia avviato e che firewall/porte siano aperti.";
            }
            if (cur instanceof NotBoundException) {
                return "Il server è raggiungibile su " + host + ":" + port
                        + " ma non ha registrato il servizio MESOS. "
                        + "Versione del server disallineata?";
            }
            cur = cur.getCause();
        }
        return "Connessione fallita verso " + host + ":" + port + ": "
                + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
    }
}