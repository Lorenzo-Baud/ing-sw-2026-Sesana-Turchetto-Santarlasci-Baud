package it.polimi.ingws.gc8.Client;

import it.polimi.ingws.gc8.View.TUI.Printer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Interactive pre-game bootstrap. Asks the user for UI flavor,
 * server host, port and nickname BEFORE the View is built or the network
 * connection is opened.
 */
public final class Bootstrap {

    private static final BufferedReader IN =
            new BufferedReader(new InputStreamReader(System.in));

    private static final String DEFAULT_HOST = "127.0.0.1";
    private static final int DEFAULT_PORT = 1099;

    private Bootstrap() { /* utility */ }

    /**
     * Runs the full bootstrap dialog.
     *
     * @return the validated user choices
     * @throws IOException if stdin closes unexpectedly
     */
    public static BootstrapConfig run() throws IOException {
        Printer.printInfo("=====================================");
        Printer.printInfo("       MESOS  -  client bootstrap");
        Printer.printInfo("=====================================");

        UiFlavor flavor = askUiFlavor();
        String host = askHost();
        int port = askPort();
        String nickname = askNickname();

        return new BootstrapConfig(flavor, host, port, nickname);
    }

    /**
     * Asks the host (first player) how many participants the match should
     * have. Called only when the server reports {@code isFirst == true}.
     *
     * @param min minimum allowed number of players (server-defined)
     * @param max maximum allowed number of players (server-defined)
     * @return the chosen number of players
     * @throws IOException if stdin closes unexpectedly
     */
    public static int askExpectedPlayers(int min, int max) throws IOException {
        Printer.printInfo("Sei il primo giocatore: scegli il numero di partecipanti.");
        return chooseInRange("Numero di giocatori (" + min + "-" + max + ")", min, max);
    }

    // Sub-prompts

    private static UiFlavor askUiFlavor() throws IOException {
        while (true) {
            Printer.printInfo("Quale interfaccia vuoi usare?");
            System.out.println("  1) TUI (terminal)");
            System.out.println("  2) GUI (non disponibile)");
            int c = chooseInRange("Scelta", 1, 2);
            if (c == 1) return UiFlavor.TUI;
            Printer.printError("GUI non ancora disponibile. Scegli TUI.");
        }
    }

    private static String askHost() throws IOException {
        Printer.printPrompt("Host del server [" + DEFAULT_HOST + "]: ");
        String line = readLine();
        return (line == null || line.isBlank()) ? DEFAULT_HOST : line.trim();
    }

    private static int askPort() throws IOException {
        while (true) {
            Printer.printPrompt("Porta [" + DEFAULT_PORT + "]: ");
            String line = readLine();
            if (line == null || line.isBlank()) return DEFAULT_PORT;
            try {
                int p = Integer.parseInt(line.trim());
                if (p > 0 && p < 65536) return p;
            } catch (NumberFormatException ignored) {
                // fall through to error
            }
            Printer.printError("Porta non valida.");
        }
    }

    private static String askNickname() throws IOException {
        while (true) {
            Printer.printPrompt("Nickname: ");
            String line = readLine();
            if (line != null && !line.isBlank()) return line.trim();
            Printer.printError("Nickname non valido.");
        }
    }

    private static int chooseInRange(String label, int min, int max) throws IOException {
        while (true) {
            Printer.printPrompt(label + " > ");
            String line = readLine();
            if (line == null) {
                throw new IOException("stdin closed during bootstrap");
            }
            try {
                int v = Integer.parseInt(line.trim());
                if (v >= min && v <= max) return v;
            } catch (NumberFormatException ignored) {
                // fall through
            }
            Printer.printError("Inserisci un numero tra " + min + " e " + max + ".");
        }
    }

    private static String readLine() throws IOException {
        return IN.readLine();
    }
}
