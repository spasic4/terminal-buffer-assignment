package com.jetbrains.terminal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Core implementation of the terminal text buffer.
 *
 * Design decisions:
 * - Separate scrollback (ArrayList) and screen (2D Array) for O(1) scrolling performance.
 * - Implements auto-scrolling when the cursor moves beyond the last line.
 * - Supports line-wrapping during text insertion/writing.
 */
public class TerminalBuffer {
    private final int width;
    private final int height;
    private final int maxScrollback;

    // Screen: The current editable grid.
    private TerminalCell[][] screen;

    // Scrollback: History of lines that are no longer editable.
    private List<TerminalCell[]> scrollback;

    // Current State
    private int cursorX = 0;
    private int cursorY = 0;
    private TerminalColor currentFg = TerminalColor.DEFAULT;
    private TerminalColor currentBg = TerminalColor.DEFAULT;
    private byte currentStyle = 0;

    public TerminalBuffer(int width, int height, int maxScrollback) {
        this.width = width;
        this.height = height;
        this.maxScrollback = maxScrollback;
        this.scrollback = new ArrayList<>();
        this.screen = new TerminalCell[height][width];
        clearScreen();
    }

    public void setAttributes(TerminalColor fg, TerminalColor bg, byte styleFlags) {
        this.currentFg = fg;
        this.currentBg = bg;
        this.currentStyle = styleFlags;
    }

    // --- Cursor Management ---

    public void setCursor(int x, int y) {
        this.cursorX = Math.max(0, Math.min(x, width - 1));
        this.cursorY = Math.max(0, Math.min(y, height - 1));
    }

    public void moveCursor(int dx, int dy) {
        setCursor(cursorX + dx, cursorY + dy);
    }

    public void moveCursorUp(int n) {
        setCursor(cursorX, cursorY - n);
    }

    public void moveCursorDown(int n) {
        setCursor(cursorX, cursorY + n);
    }

    public void moveCursorRight(int n) {
        setCursor(cursorX + n, cursorY);
    }

    public void moveCursorLeft(int n) {
        setCursor(cursorX - n, cursorY);
    }

    public int getCursorX() { return cursorX; }
    public int getCursorY() { return cursorY; }

    private void moveCursorAfterWrite() {
        cursorX++;
        if (cursorX >= width) {
            cursorX = 0;
            if (cursorY < height - 1) {
                cursorY++;
            } else {
                scrollLineUp();
            }
        }
    }

    // --- Editing Operations ---

    /**
     * Writes text starting at current cursor position, overriding content.
     * Implements line wrapping and auto-scrolling.
     */
    public void writeText(String text) {
        for (char c : text.toCharArray()) {
            screen[cursorY][cursorX] = new TerminalCell(c, currentFg, currentBg, currentStyle);
            moveCursorAfterWrite();
        }
    }

    /**
     * Pushes existing characters to the right.
     * If a character is pushed beyond the width, it wraps to the next line.
     */
    public void insertText(String text) {
        for (char c : text.toCharArray()) {
            insertSingleChar(c);
        }
    }

    private void insertSingleChar(char c) {
        TerminalCell charToInsert = new TerminalCell(c, currentFg, currentBg, currentStyle);
        TerminalCell pushedOut = null;

        // Shift characters in the current line to the right
        pushedOut = screen[cursorY][width - 1];
        System.arraycopy(screen[cursorY], cursorX, screen[cursorY], cursorX + 1, width - 1 - cursorX);
        screen[cursorY][cursorX] = charToInsert;

        moveCursorAfterWrite();
    }

    /**
     * Fills the entire current line with a specific character using current attributes.
     */
    public void fillLine(char c) {
        TerminalCell fillCell = new TerminalCell(c, currentFg, currentBg, currentStyle);
        Arrays.fill(screen[cursorY], fillCell);
    }

    /**
     * Manual insertion of an empty line at the bottom.
     */
    public void insertEmptyLineAtBottom() {
        scrollLineUp();
    }

    public void clearScreen() {
        for (int y = 0; y < height; y++) {
            Arrays.fill(screen[y], TerminalCell.EMPTY);
        }
        cursorX = 0;
        cursorY = 0;
    }

    public void clearAll() {
        scrollback.clear();
        clearScreen();
    }

    // --- Internal Logic ---

    /**
     * Moves the top line of the screen into scrollback and shifts everything up.
     */
    private void scrollLineUp() {
        // 1. Move the top line (index 0) to scrollback
        TerminalCell[] topLine = screen[0];
        if (maxScrollback > 0) {
            if (scrollback.size() >= maxScrollback) {
                scrollback.remove(0); // Remove oldest if limit reached
            }
            scrollback.add(topLine);
        }

        // 2. Shift all lines up
        for (int y = 0; y < height - 1; y++) {
            screen[y] = screen[y + 1];
        }

        // 3. Create a fresh empty line at the bottom
        screen[height - 1] = new TerminalCell[width];
        Arrays.fill(screen[height - 1], TerminalCell.EMPTY);
    }

    // --- Content Access ---

    public int getTotalLines() {
        return scrollback.size() + height;
    }

    /**
     * Returns a row by absolute index (0 is the oldest line in scrollback).
     */
    private TerminalCell[] getRowInternal(int row) {
        if (row < 0 || row >= getTotalLines()) throw new IndexOutOfBoundsException();

        if (row < scrollback.size()) {
            return scrollback.get(row);
        } else {
            return screen[row - scrollback.size()];
        }
    }

    public char getCharAt(int col, int row) {
        return getRowInternal(row)[col].character();
    }

    /**
     * TerminalCell contains all required attributes.
     */
    public TerminalCell getAttributesAt(int col, int row) {
        return getRowInternal(row)[col];
    }

    public String getLineAsString(int row) {
        TerminalCell[] line = getRowInternal(row);
        StringBuilder sb = new StringBuilder();
        for (TerminalCell cell : line) {
            sb.append(cell.character());
        }
        return sb.toString();
    }

    public String getScreenAsString() {
        StringBuilder sb = new StringBuilder();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                sb.append(screen[y][x].character());
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    public String getFullHistoryAsString() {
        StringBuilder sb = new StringBuilder();
        // Add scrollback history
        for (TerminalCell[] line : scrollback) {
            for (TerminalCell cell : line) {
                sb.append(cell.character());
            }
            sb.append("\n");
        }
        // Add current screen
        sb.append(getScreenAsString());
        return sb.toString();
    }
}
