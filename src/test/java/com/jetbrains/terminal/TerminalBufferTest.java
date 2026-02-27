package com.jetbrains.terminal;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TerminalBufferTest {

    @Test
    void cursor_shouldNotMoveBeyondScreenBounds() {
        // Arrange
        TerminalBuffer buffer = new TerminalBuffer(10, 10, 100);

        // Act
        buffer.setCursor(50, 50);

        // Assert
        assertEquals(9, buffer.getCursorX(), "Cursor X should be bounded to width - 1");
        assertEquals(9, buffer.getCursorY(), "Cursor Y should be bounded to height - 1");

        // Act 2
        buffer.moveCursor(-100, -100);

        // Assert 2
        assertEquals(0, buffer.getCursorX(), "Cursor X should not be negative");
        assertEquals(0, buffer.getCursorY(), "Cursor Y should not be negative");
    }

    @Test
    void writeText_shouldApplyCurrentAttributesAndMoveCursor() {
        // Arrange
        TerminalBuffer buffer = new TerminalBuffer(20, 10, 50);
        buffer.setAttributes(TerminalColor.RED, TerminalColor.BLACK, TerminalCell.BOLD);

        // Act
        buffer.writeText("Hello");

        // Assert
        assertEquals(5, buffer.getCursorX(), "Cursor should move 5 positions to the right");
        assertEquals(0, buffer.getCursorY(), "Cursor should remain on the same line");

        // Assert 2: Check if the first char is H and if has attributes
        TerminalCell firstCell = buffer.getAttributesAt(0, 0); // (col, row)
        assertEquals('H', firstCell.character());
        assertEquals(TerminalColor.RED, firstCell.foregroundColor());
        assertEquals(TerminalColor.BLACK, firstCell.backgroundColor());
        assertTrue(firstCell.hasStyle(TerminalCell.BOLD), "Cell should have BOLD style");
    }

    @Test
    void writeText_shouldWrapToNextLine_whenReachingRightEdge() {
        // Arrange
        TerminalBuffer buffer = new TerminalBuffer(5, 5, 10);

        // Act: "67" should be on the next line
        buffer.writeText("1234567");

        // Assert
        assertEquals(2, buffer.getCursorX(), "Cursor should be at index 2 on the new line");
        assertEquals(1, buffer.getCursorY(), "Cursor should wrap to row 1");

        // Are characters on right positions
        assertEquals('1', buffer.getCharAt(0, 0));
        assertEquals('5', buffer.getCharAt(4, 0));
        assertEquals('6', buffer.getCharAt(0, 1));
        assertEquals('7', buffer.getCharAt(1, 1));
    }

    @Test
    void scrollback_shouldPreserveHistoryAndRespectMaximumLimit() {
        // Arrange: Height is 3 lines, max history size is 2 lines.
        TerminalBuffer buffer = new TerminalBuffer(5, 3, 2);

        // Act
        buffer.writeText("AAAAA"); // Row 0
        buffer.writeText("BBBBB"); // Row 1
        buffer.writeText("CCCCC"); // Row 2 (scrollLineUp called)
        buffer.writeText("DDDDD"); // Row 2 (scrollLineUp called)
        buffer.writeText("EEEEE"); // Row 2 (scrollLineUp called)

        // Expected state in memory:
        // Scrollback[0] = "BBBBB" ("AAAAA" line was removed because the limit is 2)
        // Scrollback[1] = "CCCCC"
        // Screen[0] = "DDDDD"
        // Screen[1] = "EEEEE"
        // Screen[2] = "     "

        // Assert
        assertEquals(5, buffer.getTotalLines(), "Total tracked lines should be scrollback limit (2) + screen height (3)");

        // Assert 1: Check History
        assertEquals("BBBBB", buffer.getLineAsString(0));
        assertEquals("CCCCC", buffer.getLineAsString(1));

        // Assert 2: Check Screen (absolute rows are 2, 3, 4)
        assertEquals("DDDDD", buffer.getLineAsString(2));
        assertEquals("EEEEE", buffer.getLineAsString(3));
        assertEquals("     ", buffer.getLineAsString(4)); // Empty row made by last scrollup
    }

    @Test
    void insertText_shouldShiftCharactersToTheRight() {
        // Arrange
        TerminalBuffer buffer = new TerminalBuffer(10, 5, 10);
        buffer.writeText("12345"); // Screen now has "12345     " on 0 row

        // Act
        buffer.setCursor(0, 0);
        buffer.insertText("A"); // Inserting 'A' on position 0

        // Assert
        // Expect "12345" to shift right and become "A12345"
        assertEquals("A12345    ", buffer.getLineAsString(0));
        assertEquals(1, buffer.getCursorX(), "Cursor should move 1 step right after insert");
    }

    @Test
    void clearScreen_shouldEraseScreenContentAndResetCursor_butKeepScrollback() {
        // Arrange
        TerminalBuffer buffer = new TerminalBuffer(5, 2, 10);
        buffer.writeText("AAAAA"); // Row 0
        buffer.writeText("BBBBB"); // Row 1
        buffer.writeText("CCCCC"); // Row 2 (scrollLineUp called (Row 3 is "     "), AAAAA goes to scrollback)

        // Assert 1: Make sure "AAAAA" is in scrollback
        assertEquals("AAAAA", buffer.getLineAsString(0));

        // Act
        buffer.clearScreen();

        // Assert 2
        assertEquals(0, buffer.getCursorX(), "Cursor X should reset to 0");
        assertEquals(0, buffer.getCursorY(), "Cursor Y should reset to 0");

        // Assert 3: Is screen empty?
        String emptyScreen = "     \n     \n";
        assertEquals(emptyScreen, buffer.getScreenAsString());

        // Assert 4
        assertEquals("AAAAA", buffer.getLineAsString(0));
        assertEquals(4, buffer.getTotalLines()); // 2 scrollback + 2 screen
    }

    @Test
    void clearAll_shouldEraseEverythingIncludingHistory() {
        // Arrange
        TerminalBuffer buffer = new TerminalBuffer(5, 2, 10);
        buffer.writeText("AAAAA");
        buffer.writeText("BBBBB");
        buffer.writeText("CCCCC"); // AAAAA goes to scrollback

        // Act
        buffer.clearAll();

        // Assert
        assertEquals(2, buffer.getTotalLines(), "Total lines should be just the empty screen height (2)");
        assertEquals("     \n     \n", buffer.getFullHistoryAsString());
    }

    @Test
    void resize_shouldResizeExpandBuffer() {
        // Arrange
        TerminalBuffer buffer = new TerminalBuffer(2, 2, 10);
        buffer.writeText("AB");
        buffer.writeText("CD");

        // Act
        buffer.resize(4, 4);

        // Assert
        assertEquals(4, buffer.getScreenAsString().split("\n")[0].length());
        assertEquals('A', buffer.getCharAt(0, 0));
        assertEquals('C', buffer.getCharAt(0, 1));
        assertEquals(' ', buffer.getCharAt(3, 3)); // New area is empty
    }

    @Test
    void wideCharacter_shouldOccupyTwoCells_andSetRightHalfFlag() {
        // Arrange
        TerminalBuffer buffer = new TerminalBuffer(10, 5, 10);

        // Act
        buffer.writeText("中");

        // Assert 1: Cursor move 2 times to right
        assertEquals(2, buffer.getCursorX(), "Cursor should move 2 positions for a wide character");

        // Assert 2: Check if left cell has wide character
        TerminalCell leftCell = buffer.getAttributesAt(0, 0);
        assertEquals('中', leftCell.character());
        assertFalse(leftCell.hasStyle(TerminalCell.WIDE_RIGHT_HALF), "Left cell must NOT have the right-half flag");

        // Assert 3: Check if right cell has '\0' character
        TerminalCell rightCell = buffer.getAttributesAt(1, 0);
        assertEquals('\0', rightCell.character(), "Right half should contain the null character placeholder");
        assertTrue(rightCell.hasStyle(TerminalCell.WIDE_RIGHT_HALF), "Right half MUST have the WIDE_RIGHT_HALF flag");
    }

    @Test
    void cursor_shouldSnapLeft_whenLandingOnWideCharacterRightHalf() {
        // Arrange
        TerminalBuffer buffer = new TerminalBuffer(10, 5, 10);
        buffer.writeText("中"); // Cursor ends up on pos x = 2

        // Act: Trying to set cursor on the wide right half position
        buffer.setCursor(1, 0);

        // Assert: Cursor automatically slides to column 0
        assertEquals(0, buffer.getCursorX(), "Cursor should automatically snap to the left half (index 0)");
        assertEquals(0, buffer.getCursorY());
    }
}