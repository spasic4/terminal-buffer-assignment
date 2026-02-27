package com.jetbrains.terminal;

/**
 * 16 standard colors + Default.
 */
public enum TerminalColor {
    DEFAULT,
    BLACK, RED, GREEN, YELLOW, BLUE, MAGENTA, CYAN, WHITE,
    BRIGHT_BLACK, BRIGHT_RED, BRIGHT_GREEN, BRIGHT_YELLOW,
    BRIGHT_BLUE, BRIGHT_MAGENTA, BRIGHT_CYAN, BRIGHT_WHITE;

    /**
     * Utility method for future integration.
     * Maps standard ANSI escape codes (e.g., from shell output) to internal terminal colors.
     */
    public static TerminalColor fromAnsi(int code) {
        return switch (code) {
            case 30, 40 -> BLACK;
            case 31, 41 -> RED;
            case 32, 42 -> GREEN;
            case 33, 43 -> YELLOW;
            case 34, 44 -> BLUE;
            case 35, 45 -> MAGENTA;
            case 36, 46 -> CYAN;
            case 37, 47 -> WHITE;
            case 90, 100 -> BRIGHT_BLACK;
            case 91, 101 -> BRIGHT_RED;
            case 92, 102 -> BRIGHT_GREEN;
            case 93, 103 -> BRIGHT_YELLOW;
            case 94, 104 -> BRIGHT_BLUE;
            case 95, 105 -> BRIGHT_MAGENTA;
            case 96, 106 -> BRIGHT_CYAN;
            case 97, 107 -> BRIGHT_WHITE;
            default -> DEFAULT;
        };
    }
}
