Terminal Text Buffer Implementation
========

This repository contains the core data structure for a terminal emulator, developed for the JetBrains Internship assignment.

Below is the explanation of the solution, the trade-offs considered during development, and the design decisions made to ensure optimal performance and maintainability.

Design Decisions & Trade-offs
-------
1. Memory Optimization vs. OOP Purity (The byte Bitmask)

A terminal buffer can easily contain millions of cells when factoring in a large scrollback history. Storing text styles (Bold, Italic, Underline) as Set<StyleEnum> or objects would create massive memory overhead and cause severe Garbage Collection (GC) pressure.
Trade-off: I sacrificed strict OOP paradigms for system-level performance by using a primitive byte bitmask for styleFlags. This reduces the memory footprint per cell to an absolute minimum while allowing blazing-fast bitwise operations.

2. Type Safety vs. Extreme Memory Saving (The TerminalColor Enum)

While styles use bitmasks, I chose to represent colors using the TerminalColor enum rather than primitive byte constants.
Trade-off: Although using byte for colors would save a few extra megabytes across millions of cells, I prioritized Type Safety to prevent the "Primitive Obsession" anti-pattern. Enums prevent invalid colors from being passed at compile-time, which is crucial for a stable rendering engine. I also included a fromAnsi mapping utility to prepare the buffer for the next logical phase: ANSI escape sequence parsing.

3. O(1) Scrolling Performance

Instead of copying millions of characters cell-by-cell during scrolling, the buffer strictly moves references to TerminalCell[] row arrays.
Decision: The screen is backed by a 2D array (for fast absolute x,y indexing), and the scrollback is an ArrayList<TerminalCell[]>. When the screen scrolls, the top row's reference is simply added to the scrollback list, and a new empty row is created at the bottom. This ensures scrolling remains an O(1) operation, regardless of the terminal width.

4. Fast Text Insertion

For the insertText requirement (which pushes existing characters to the right), I utilized System.arraycopy. This native JVM method is significantly faster than manually shifting elements with a for loop, ensuring smooth performance even when editing long lines.

Bonus Features Implemented
-------

Wide Characters (CJK / Emoji) Support:
I implemented the bonus challenge for characters that occupy 2 cells.

When a wide character is written, the engine places the actual character in the left cell and a \0 (null) placeholder in the right cell.

The right cell receives a special WIDE_RIGHT_HALF bitmask flag.

Cursor Snapping: The setCursor method includes boundary logic that detects if the cursor lands on a WIDE_RIGHT_HALF cell. If it does, the cursor automatically snaps to the left (the main character), preventing the user from accidentally typing inside the right half of a wide character.

Future Improvements
-------

If I had more time, I would implement the following improvements:

Terminal Resizing (Reflowing): Implementing the resize bonus challenge. The main complexity here is text reflowing — recalculating word wraps when the width shrinks and expanding them when the width grows.

Full Unicode Code Point Support: Currently, the TerminalCell uses a primitive char (16-bit). While this covers basic CJK characters, full Emoji support (which often requires surrogate pairs) would require refactoring the cell to store an int (Code Point) or a String.

Primitive-Backed Grid: For an absolute extreme memory optimization (e.g., billions of cells), the entire grid could be flattened into a single primitive long array where each long encodes the character, style flags, and color data into a single 64-bit value using bitwise packing. This eliminates all object overhead and pointer indirection, maximizing cache locality and minimizing GC pressure at the cost of code readability and type-safety.

ANSI Escape Sequence Parsing: The groundwork for this feature is already laid — the TerminalColor.fromAnsi() mapping utility was designed with this phase in mind. The next step would be implementing a state-machine parser that processes incoming byte streams, detects ESC [ sequences, and translates them into the appropriate buffer operations such as cursor movement, color changes, and style updates.
