package org.ojas.cli;

/**
 * ═══════════════════════════════════════════════════════════════════════
 *  OJAS CONSOLE — xAI-Inspired Terminal UI
 *
 *  Design language drawn from xAI's visual system:
 *   • Near-black canvas with white ink (dark terminal native)
 *   • Pill-shaped dividers using Unicode box-drawing characters
 *   • Monospace uppercase eyebrow labels (the GeistMono aesthetic)
 *   • Hairline borders — no heavy decorations
 *   • Engineered-cosmic, unmarketed feel
 *
 *  Colors reference (from DESIGN-x.ai.md):
 *   canvas:        #0a0a0a (terminal default black)
 *   ink:           #ffffff (terminal default white)
 *   accent-sunset: #ff7a17 (ANSI orange for highlights)
 *   accent-dusk:   #7c3aed (ANSI purple for system labels)
 *   body-mid:      #7d8187 (ANSI dim for secondary text)
 *   hairline:      #212327 (subtle dividers)
 * ═══════════════════════════════════════════════════════════════════════
 */
public class OjasConsole {

    // ── ANSI Color Codes (xAI palette mapped to terminal) ───────────
    private static final String RESET   = "\033[0m";
    private static final String WHITE   = "\033[97m";   // ink
    private static final String DIM     = "\033[90m";   // body-mid / mute
    private static final String ORANGE  = "\033[38;5;208m"; // accent-sunset #ff7a17
    private static final String PURPLE  = "\033[38;5;99m";  // accent-dusk #7c3aed
    private static final String CYAN    = "\033[96m";   // accent-breeze
    private static final String GREEN   = "\033[92m";
    private static final String RED     = "\033[91m";
    private static final String BOLD    = "\033[1m";

    // ── Branding Constants ──────────────────────────────────────────
    private static final int CONSOLE_WIDTH = 72;

    /**
     * Print the boot splash — engineered-cosmic aesthetic.
     */
    public static void printSplash() {
        System.out.println();
        System.out.println(DIM + hairline() + RESET);
        System.out.println();
        System.out.println(WHITE + BOLD + "   ██████╗      ██╗ █████╗ ███████╗" + RESET);
        System.out.println(WHITE + BOLD + "  ██╔═══██╗     ██║██╔══██╗██╔════╝" + RESET);
        System.out.println(WHITE + BOLD + "  ██║   ██║     ██║███████║███████╗" + RESET);
        System.out.println(WHITE + BOLD + "  ██║   ██║██   ██║██╔══██║╚════██║" + RESET);
        System.out.println(WHITE + BOLD + "  ╚██████╔╝╚█████╔╝██║  ██║███████║" + RESET);
        System.out.println(WHITE + BOLD + "   ╚═════╝  ╚════╝ ╚═╝  ╚═╝╚══════╝" + RESET);
        System.out.println();
        System.out.println(eyebrow("LOCAL-FIRST ENTERPRISE AI AGENT"));
        System.out.println();
        System.out.println(DIM + "  Built from scratch in Java · LangChain4j · Google Gemini" + RESET);
        System.out.println(DIM + "  Not a wrapper — a true agentic system with autonomous tools" + RESET);
        System.out.println();
        System.out.println(DIM + hairline() + RESET);
        System.out.println();
    }

    /**
     * Print system boot status messages.
     */
    public static void printBootStatus(String modelName, int memoryWindow) {
        System.out.println(eyebrow("SYSTEM STATUS"));
        System.out.println();
        printStatusLine("MODEL",    modelName);
        printStatusLine("MEMORY",   memoryWindow + "-message sliding window (FIFO)");
        printStatusLine("TOOLS",    "FileSystem · ProcessBuilder · RAG");
        printStatusLine("SECURITY", "BYOK — key loaded from environment");
        System.out.println();
    }

    /**
     * Print RAG indexing status.
     */
    public static void printRagStatus(String directory, int fileCount, long durationMs) {
        System.out.println(eyebrow("RAG INDEX"));
        System.out.println();
        printStatusLine("DIRECTORY", directory);
        printStatusLine("INDEXED",   fileCount + " files");
        printStatusLine("TIME",      durationMs + "ms");
        System.out.println();
    }

    /**
     * Print a status key-value line in the xAI mono style.
     */
    public static void printStatusLine(String key, String value) {
        String paddedKey = String.format("  %-12s", key.toUpperCase());
        System.out.println(PURPLE + paddedKey + RESET + DIM + " → " + RESET + WHITE + value + RESET);
    }

    /**
     * Print the user input prompt.
     */
    public static void printPrompt() {
        System.out.print(ORANGE + "  ▸ " + WHITE + "you" + DIM + " › " + RESET);
    }

    /**
     * Print the agent response header.
     */
    public static void printResponseHeader() {
        System.out.println();
        System.out.println(CYAN + "  ◆ " + WHITE + "ojas" + RESET);
    }

    /**
     * Print the agent response body.
     */
    public static void printResponse(String response) {
        printResponseHeader();
        // Indent each line of the response for visual alignment
        String[] lines = response.split("\n");
        for (String line : lines) {
            System.out.println(DIM + "    " + RESET + line);
        }
        System.out.println();
    }

    /**
     * Print an error message.
     */
    public static void printError(String message) {
        System.out.println(RED + "  ✘ ERROR: " + RESET + message);
    }

    /**
     * Print a success message.
     */
    public static void printSuccess(String message) {
        System.out.println(GREEN + "  ✔ " + RESET + message);
    }

    /**
     * Print an info/system message.
     */
    public static void printInfo(String message) {
        System.out.println(DIM + "  ℹ " + message + RESET);
    }

    /**
     * Print help / available commands.
     */
    public static void printHelp() {
        System.out.println();
        System.out.println(eyebrow("COMMANDS"));
        System.out.println();
        System.out.println(WHITE + "  /rag <dir>    " + DIM + "Index a local directory for RAG search" + RESET);
        System.out.println(WHITE + "  /ingest <file>" + DIM + "Import a ChatGPT JSON export or transcript" + RESET);
        System.out.println(WHITE + "  /model <name> " + DIM + "Switch Gemini model (e.g., gemini-2.5-flash)" + RESET);
        System.out.println(WHITE + "  /clear        " + DIM + "Clear conversation memory" + RESET);
        System.out.println(WHITE + "  /help         " + DIM + "Show this help menu" + RESET);
        System.out.println(WHITE + "  /exit         " + DIM + "Quit Ojas" + RESET);
        System.out.println();
        System.out.println(DIM + hairline() + RESET);
        System.out.println();
    }

    /**
     * Print shutdown message.
     */
    public static void printShutdown() {
        System.out.println();
        System.out.println(DIM + hairline() + RESET);
        System.out.println();
        System.out.println(eyebrow("SESSION TERMINATED"));
        System.out.println(DIM + "  Ojas agent shut down cleanly." + RESET);
        System.out.println();
    }

    // ── xAI Design Primitives ───────────────────────────────────────

    /**
     * GeistMono-style uppercase tracked eyebrow label.
     * Maps to {typography.caption-mono} from DESIGN-x.ai.md:
     *   fontSize: 14px, letterSpacing: 1.4px, uppercase
     */
    private static String eyebrow(String text) {
        // Simulate tracked uppercase mono by inserting spaces between chars
        StringBuilder spaced = new StringBuilder("  ");
        for (char c : text.toUpperCase().toCharArray()) {
            spaced.append(c).append(' ');
        }
        return PURPLE + spaced.toString().trim() + RESET;
    }

    /**
     * Hairline divider — the only elevation cue in xAI's system.
     * Maps to {components.divider-hairline} → 1px solid {colors.hairline}
     */
    private static String hairline() {
        return "  " + "─".repeat(CONSOLE_WIDTH);
    }
}
