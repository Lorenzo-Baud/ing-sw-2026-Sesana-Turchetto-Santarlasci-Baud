package it.polimi.ingws.gc8.View.TUI;

/**
 * Costanti ANSI e helper di stampa per la TUI.
 *
 * La TUI lavora in modalità append-mode (nessun clear screen, nessuna
 * gestione raw-mode del terminale): ogni ridisegno scrive una nuova schermata
 * in fondo allo stdout. Questo evita le trappole tipiche di Windows cmd
 * (che non supporta ANSI di default su installazioni vecchie) e resta
 * compatibile con qualunque terminale.
 *
 * Tutti i metodi sono thread-safe perché delegano a {@code System.out}
 * (PrintStream è internamente sincronizzato).
 */
public final class Printer {

    private Printer() { /* classe di utilità */ }

    // Reset
    public static final String RESET  = "\u001B[0m";

    // Foreground
    public static final String BLACK  = "\u001B[30m";
    public static final String RED    = "\u001B[31m";
    public static final String GREEN  = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE   = "\u001B[34m";
    public static final String PURPLE = "\u001B[35m";
    public static final String CYAN   = "\u001B[36m";
    public static final String WHITE  = "\u001B[37m";

    // Stili
    public static final String BOLD   = "\u001B[1m";
    public static final String DIM    = "\u001B[2m";

    // Background
    public static final String BG_BLACK  = "\u001B[40m";
    public static final String BG_RED    = "\u001B[41m";
    public static final String BG_GREEN  = "\u001B[42m";
    public static final String BG_YELLOW = "\u001B[43m";
    public static final String BG_BLUE   = "\u001B[44m";
    public static final String BG_WHITE  = "\u001B[47m";

    /** Info generica (cyan). */
    public static void printInfo(String msg) {
        System.out.println(CYAN + msg + RESET);
    }

    /** Messaggio di successo (green). */
    public static void printSuccess(String msg) {
        System.out.println(GREEN + msg + RESET);
    }

    /** Errore (red). */
    public static void printError(String msg) {
        System.out.println(RED + "[ERR] " + msg + RESET);
    }

    /** Prompt in bold + senza newline finale (così l'utente scrive sulla stessa riga). */
    public static void printPrompt(String msg) {
        System.out.print(BOLD + msg + RESET);
    }

    /** Intestazione di sezione, preceduta da riga vuota. */
    public static void printHeader(String title) {
        System.out.println();
        System.out.println(BOLD + YELLOW + "=== " + title + " ===" + RESET);
    }

    /** Separatore orizzontale lungo {@code width} caratteri. */
    public static void printSeparator(int width) {
        System.out.println("-".repeat(Math.max(0, width)));
    }
}
