package it.polimi.ingws.gc8.View.TUI;

/**
 * ANSI constants and printing helpers for the TUI.
 *
 * The TUI works in append-mode (no clear screen, no raw-mode terminal
 * management): every redraw writes a new screen to the bottom of stdout.
 * This avoids the typical traps of Windows cmd (which doesn't support ANSI
 * by default on older installations) and remains compatible with any terminal.
 *
 * All methods are thread-safe because they delegate to {@code System.out}
 * (PrintStream is internally synchronized).
 */
public final class Printer {

    private Printer() { /* utility class */ }

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

    // Styles
    public static final String BOLD   = "\u001B[1m";
    public static final String DIM    = "\u001B[2m";

    // Background
    public static final String BG_BLACK  = "\u001B[40m";
    public static final String BG_RED    = "\u001B[41m";
    public static final String BG_GREEN  = "\u001B[42m";
    public static final String BG_YELLOW = "\u001B[43m";
    public static final String BG_BLUE   = "\u001B[44m";
    public static final String BG_WHITE  = "\u001B[47m";

    /** Generic info (cyan). */
    public static void printInfo(String msg) {
        System.out.println(CYAN + msg + RESET);
    }

    /** Success message (green). */
    public static void printSuccess(String msg) {
        System.out.println(GREEN + msg + RESET);
    }

    /** Error (red). */
    public static void printError(String msg) {
        System.out.println(RED + "[ERR] " + msg + RESET);
    }

    /** Prompt in bold + no trailing newline (so the user types on the same line). */
    public static void printPrompt(String msg) {
        System.out.print(BOLD + msg + RESET);
    }

    /** Section header, preceded by a blank line. */
    public static void printHeader(String title) {
        System.out.println();
        System.out.println(BOLD + YELLOW + "=== " + title + " ===" + RESET);
    }

    /** Horizontal separator of {@code width} characters. */
    public static void printSeparator(int width) {
        System.out.println("-".repeat(Math.max(0, width)));
    }
}
