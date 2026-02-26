package com.jetbrains.terminal;

/**
 * Represents a single cell in terminal.
 *
 * Design decisions:
 * - Uses Java Record for immutability and concise syntax.
 * - Styles are stored as a bitmask (byte) to minimize memory usage,
 *   which is critical when handling large scrollback buffers.
 * - Colors are represented by an enum to support the standard 16-color palette
 *   plus the default terminal theme color.
 * - Currently uses 'char' for simplicity; for full Unicode/Emoji support,
 *   this could be refactored to store char code as 'int'.
 */
public record TerminalCell(
        char character,
        TerminalColor foregroundColor,
        TerminalColor backgroundColor,
        byte styleFlags
) {
    public static final byte BOLD = 1 << 0;      // 0001
    public static final byte ITALIC = 1 << 1;    // 0010
    public static final byte UNDERLINE = 1 << 2; // 0100

    // Default empty cell
    public static final TerminalCell EMPTY = new TerminalCell(' ', TerminalColor.DEFAULT, TerminalColor.DEFAULT, (byte) 0);

    public boolean hasStyle(byte style) {
        return (styleFlags & style) != 0;
    }
}