package dev.fusemc.quelle.position;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/// A position in a [CharSequence].
///
/// ---
/// A `CharPosition` represents a certain position in a `CharSequence`, defined through
/// the absolute offset, the column, and the line number of a Unicode **code point** in
/// the source sequence [S].
///
/// A `CharPosition` may point to a position outside the associated source sequence.
/// It is then deemed to be {@linkplain #isVirtual() virtual}.
///
/// @param <S> The type of the source.
/// @since `0.1.0`
public final class CharPosition<S extends CharSequence> implements Comparable<CharPosition<S>> {

    private final @NotNull S source;
    public final int offset;
    public final int column;
    public final int line;

    private CharPosition(@NotNull S source, int offset, int column, int line) {
        this.source = Objects.requireNonNull(source);
        this.offset = offset;
        this.column = column;
        this.line = line;
    }

    /// Constructs a raw `CharPosition` from the given offsets.
    ///
    /// @since `0.1.0`
    public static <S extends CharSequence> @NotNull CharPosition<S> raw(@NotNull S source, int offset, int column, int line) {
        Objects.requireNonNull(source);
        return new CharPosition<>(source, offset, column, line);
    }

    /// Computes a `CharPosition` from the given **absolute** offset.
    ///
    /// ---
    /// Constructs a `CharPosition` from the given offset, by computing the column
    /// and line offsets manually.
    ///
    /// The following code computes the `CharPosition` of the letter 'M':
    ///
    /// ```java
    /// var source   = "Bonjour, Marie!";
    /// var position = CharPosition.compute(source, source.indexOf('M'));
    /// ```
    ///
    /// @since `0.1.0`
    public static <S extends CharSequence> @NotNull CharPosition<S> compute(@NotNull S source, int offset) {
        Objects.requireNonNull(source);
        int column = 1, line = 1;
        for (var i = 0; i < offset;) {
            var c = Character.codePointAt(source, i);
            i += Character.charCount(c);
            if (c == '\n' || c == '\r') {
                if (c == '\r' && i < source.length() && Character.codePointAt(source, i) == '\n')
                    i++;
                column = 1;
                line++;
                continue;
            }
            column++;
        }
        return new CharPosition<>(source, offset, column, line);
    }

    /// Determine if the `CharPosition` is _virtual_.
    ///
    /// ---
    /// A `CharPosition` is said to be _virtual_ if it points to a position outside the bounds
    /// of the associated source.
    ///
    /// @since `0.1.0`
    public boolean isVirtual() {
        return this.offset < 0 || this.offset >= this.source.length();
    }

    /// Return the Unicode **code point** the `CharPosition` is pointing to.
    ///
    /// @since `0.1.0`
    public int codePointAt() {
        return Character.codePointAt(this.source, this.offset);
    }

    /// Return the UTF-16 **character** the `CharPosition` is pointing to.
    ///
    /// @since `0.1.0`
    public char charAt() {
        return this.source.charAt(this.offset);
    }

    /// Determine if the `CharPosition` is _behind_ the given offset.
    ///
    /// @since `0.1.0`
    public boolean isBehind(int offset) {
        return this.offset < offset;
    }

    /// Determines if the `CharPosition` is _at_ the given offset.
    ///
    /// @since `0.1.0`
    public boolean isAt(int offset) {
        return this.offset == offset;
    }

    /// Determines if the `CharPosition` is _ahead_ of the given offset.
    ///
    /// @since `0.1.0`
    public boolean isAhead(int offset) {
        return this.offset > offset;
    }

    @Override
    public int compareTo(@NotNull CharPosition<S> other) {
        Objects.requireNonNull(other);
        return Integer.compare(this.offset, other.offset);
    }

    /// Return the string representation of the `CharPosition`.
    ///
    /// ---
    /// A `CharPosition` is stringified as a line and a column, separated by a colon:
    ///
    /// ```
    /// line:column
    /// ```
    ///
    /// @since `0.1.0`
    @Override
    public @NotNull String toString() {
        return String.format("%d:%d", this.line, this.column);
    }
}
