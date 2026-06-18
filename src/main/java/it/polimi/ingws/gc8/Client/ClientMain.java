package it.polimi.ingws.gc8.Client;

import it.polimi.ingws.gc8.Controller.MainController;
import it.polimi.ingws.gc8.Network.Connection.RMI.RMIServerConnection;
import it.polimi.ingws.gc8.Network.Connection.ServerConnection;
import it.polimi.ingws.gc8.Network.Connection.Socket.SocketServerConnection;
import it.polimi.ingws.gc8.Network.DTO.JoinResult;
import it.polimi.ingws.gc8.Network.DTO.MatchInfo;
import it.polimi.ingws.gc8.View.GUI.GuiView;
import it.polimi.ingws.gc8.View.TUI.Printer;
import it.polimi.ingws.gc8.View.TUI.TuiView;
import it.polimi.ingws.gc8.View.View;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.util.List;
import java.util.Scanner;

public class ClientMain {

    public static void main(String[] args) {
        BootstrapConfig cfg;
        try {
            cfg = Bootstrap.run();
        } catch (Exception e) {
            System.err.println("[Client] Bootstrap failed: " + e.getMessage());
            return;
        }

        if (cfg.uiFlavor() == UiFlavor.GUI) {
            View view = createView(UiFlavor.GUI);
            view.run();
            return;
        }

        ServerConnection connection = createConnection(cfg.transport());
        View view = createView(cfg.uiFlavor());
        ClientController controller = new ClientController(connection, view);

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
            Printer.printSuccess("[Client] Connesso a " + cfg.host() + ":" + cfg.port());

            controller.start();

            JoinResult joinResult = runMatchSelection(connection, cfg.nickname());
            view.setNickname(cfg.nickname());
            view.setPlayerId(joinResult.playerId());

            Printer.printSuccess("[Client] In partita '" + joinResult.matchId()
                    + "' (playerId=" + joinResult.playerId()
                    + (joinResult.isHost() ? ", host" : "") + ")");

            if (joinResult.reconnected()) {
                // FA2/FA3: rientro in una partita già avviata — niente setup lobby,
                // si attende lo snapshot completo dal server.
                Printer.printInfo("Riconnesso alla partita in corso. Ripristino in corso…");
            } else if (joinResult.isHost()) {
                int n = Bootstrap.askExpectedPlayers(
                        MainController.MIN_PLAYERS, MainController.MAX_PLAYERS);
                connection.setExpectedPlayers(n);
                Printer.printInfo("Lobby configurata per " + n + " giocatori. In attesa degli altri…");
            } else {
                Printer.printInfo("Sei in lobby, in attesa che la partita parta…");
            }

            view.run();

        } catch (Exception e) {
            Printer.printError("[Client] " + e.getMessage());
        } finally {
            controller.stop();
            connection.disconnect();
        }
    }

    private static JoinResult runMatchSelection(ServerConnection connection, String nickname) throws Exception {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            List<MatchInfo> matches = connection.listMatches();

            Printer.printInfo("=== Selezione Partita ===");
            if (matches.isEmpty()) {
                Printer.printInfo("Nessuna partita disponibile.");
            } else {
                for (int i = 0; i < matches.size(); i++) {
                    MatchInfo m = matches.get(i);
                    Printer.printInfo((i + 1) + ") " + m.matchId()
                            + " [host: " + m.hostNickname()
                            + ", giocatori: " + m.currentPlayers()
                            + "/" + (m.expectedPlayers() > 0 ? m.expectedPlayers() : "?") + "]");
                }
            }
            Printer.printInfo("[N] Nuova partita | [J <nome>] Unisciti | [R] Aggiorna");
            System.out.print("> ");

            String line = scanner.nextLine().trim();
            if (line.equalsIgnoreCase("N")) {
                System.out.print("Nome della partita: ");
                String matchName = scanner.nextLine().trim();
                try {
                    return connection.createMatch(matchName, nickname);
                } catch (Exception e) {
                    Printer.printError(e.getMessage());
                }
            } else if (line.toUpperCase().startsWith("J ")) {
                String matchId = line.substring(2).trim();
                try {
                    return connection.joinMatch(matchId, nickname);
                } catch (Exception e) {
                    Printer.printError(e.getMessage());
                }
            } else if (line.equalsIgnoreCase("R")) {
                continue;
            } else if (!matches.isEmpty()) {
                try {
                    int idx = Integer.parseInt(line) - 1;
                    if (idx >= 0 && idx < matches.size()) {
                        return connection.joinMatch(matches.get(idx).matchId(), nickname);
                    }
                } catch (NumberFormatException ignored) {}
                Printer.printError("Comando non riconosciuto.");
            }
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
            case "socket" -> new SocketServerConnection();
            default -> throw new IllegalArgumentException("Unknown transport: " + transport);
        };
    }

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