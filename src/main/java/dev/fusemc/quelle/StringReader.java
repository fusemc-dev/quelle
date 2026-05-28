package dev.fusemc.quelle;

import dev.fusemc.quelle.position.CharPosition;
import dev.fusemc.quelle.position.CharRange;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.IntPredicate;

public class StringReader<S extends CharSequence> {

    private final @NotNull S source;
    private int position;
    private int column;
    private int line;

    public StringReader(@NotNull S source) {
        this(Objects.requireNonNull(source), 0, 1, 1);
    }

    @ApiStatus.Internal
    private StringReader(@NotNull S source,
                         int position,
                         int column,
                         int line) {
        this.source = Objects.requireNonNull(source);
        this.position = position;
        this.column = column;
        this.line = line;
    }

    public static boolean isDigit(int c) {
        return c >= '0' && c <= '9';
    }

    /// Returns the [CharPosition] of the `StringReader`.
    ///
    /// @since `0.1.0`
    public @NotNull CharPosition<S> position() {
        return CharPosition.raw(this.source, this.position, this.column, this.line);
    }

    /// Compute a [CharRange] from the given [CharPosition] to the current position of the `StringReader`.
    ///
    /// ---
    ///
    /// The following code computes the [CharRange] of an identifier:
    ///
    /// ```java
    /// var position   = reader.position();
    /// var identifier = reader.readIdentifier();
    /// var range      = reader.range(position);
    /// ```
    ///
    /// @since `0.1.0`
    public @NotNull CharRange<S> range(@NotNull CharPosition<S> other) {
        return new CharRange<>(this.source, other, this.position());
    }

    /// Compute a [CharRange] relative to the current position of the `StringReader`.
    ///
    /// ---
    ///
    /// Computes a [CharRange] ranging from the current position of the `StringReader` to `length` Unicode
    /// **code points** ahead.
    ///
    /// The following code computes the [CharRange] of an escape sequence:
    ///
    /// ```java
    /// var src    = "foo\\nbar";
    /// var reader = new StringReader<>(src);
    /// for (; reader.canRead(); reader.read())
    ///     if (reader.isAt('\\'))
    ///         return reader.relativeRange(2);
    /// ```
    ///
    /// @since `0.1.0`
    public @NotNull CharRange<S> relativeRange(int length) {
        var position = this.position;
        var column   = this.column;
        var line     = this.line;
        while (length-- > 0) {
            var c = Character.codePointAt(this.source, position);
            position += Character.charCount(c);
            if (c == '\n' || c == '\r') {
                if (c == '\r' && position < this.source.length() && Character.codePointAt(this.source, position) == '\n')
                    position++;
                column = 1;
                line++;
                continue;
            }
            column++;
        }
        return new CharRange<>(this.source, this.position(), CharPosition.raw(
                this.source, position, column, line
        ));
    }

    /// Compute the [CharRange] of the code point the `StringReader` is pointing to.
    ///
    /// ---
    /// Computes the [CharRange] of the code point at the current position of the `StringReader`.
    /// When pointing to a line break, the range will span to the beginning of the next line.
    ///
    /// The following code computes the [CharRange] of the letter 'M':
    ///
    /// ```java
    /// var reader = new StringReader<>("Bonjour, Marie!");
    /// while (!reader.isAt('M'))
    ///     reader.read();
    /// var range  = reader.pointRange();
    /// ```
    ///
    /// @since `0.1.0`
    public @NotNull CharRange<S> pointRange() {
        var position = this.position();
        var c = this.peek();
        if (c == '\n')
            return new CharRange<>(this.source, position, CharPosition.raw(
                    this.source, this.position + 1, 1, this.line + 1
            ));
        return new CharRange<>(this.source, position, CharPosition.raw(
                this.source, this.position + Character.charCount(c), this.column + 1, this.line
        ));
    }

    /// Read a quoted string.
    ///
    /// ---
    /// Reads a quoted string literal from the source. The literal is expected to
    /// begin and end with the given `quote` character. Similar to other C-style string literals,
    /// **backslashes** ('/') may be used to escape the following characters within the literal:
    ///
    /// | Escape Sequence | Substitution    | ASCII   |
    /// | :-------------: | :-------------: | :----:  |
    /// | `\n`            | Newline         | `0x0A`  |
    /// | `\r`            | Carriage Return | `0x0D`  |
    /// | `\t`            | Tab             | `0x09`  |
    /// | `\b`            | Backspace       | `0x08`  |
    /// | `\\\\`          | Backslash       | `0x5C`  |
    /// | `\\` + `quote`  | Literal `quote` | `quote` |
    ///
    /// The following code reads a string literal `'foo'` from the source:
    ///
    /// ```java
    /// var reader  = new StringReader<>("'foo'");
    /// var literal = reader.readString('\'');
    /// ```
    ///
    /// @since `0.1.0`
    public @NotNull String readString(int quote) {
        if (this.isAt(quote)) {
            var buffer = new StringBuilder();
            var position = this.position();
            this.read();
            while (this.canRead()) {
                var c = this.peek();
                if (c == quote) {
                    this.read();
                    return buffer.toString();
                }
                if (c == '\\') {
                    var d = this.peek(1);
                    if (d == quote) {
                        buffer.append(quote);
                        this.read(2);
                        continue;
                    }
                    switch (d) {
                        case '\\' -> {
                            buffer.append('\\');
                            this.read(2);
                            continue;
                        }
                        case 'n' -> {
                            buffer.append('\n');
                            this.read(2);
                            continue;
                        }
                        case 'r' -> {
                            buffer.append('\r');
                            this.read(2);
                            continue;
                        }
                        case 't' -> {
                            buffer.append('\t');
                            this.read(2);
                            continue;
                        }
                        case 'b' -> {
                            buffer.append('\b');
                            this.read(2);
                            continue;
                        }
                        default -> throw new Diagnostic(String.format("Unrecognized escape sequence '\\%c' in a string literal.", d), this.relativeRange(2));
                    }
                }
                buffer.appendCodePoint(this.read());
            }
            throw new Diagnostic("Encountered an unterminated string literal.", this.range(position));
        }
        throw new Diagnostic(String.format("Expected a string literal beginning with '%c'.", quote), this.pointRange());
    }

    /// Read an identifier.
    ///
    /// ---
    /// Reads an identifier from the source. An identifier is defined as a sequence
    /// of one or more Unicode code points that satisfy the given `predicate`.
    ///
    /// The following code reads an **alphanumeric** identifier:
    ///
    /// ```java
    /// var reader     = new StringReader<>("foo");
    /// var identifier = reader.readIdentifier(Character::isLetterOrDigit);
    /// ```
    ///
    /// @since `0.1.0`
    ///
    public @NotNull String readIdentifier(@NotNull IntPredicate predicate) {
        Objects.requireNonNull(predicate);
        if (this.isAt(predicate)) {
            var buffer = new StringBuilder();
            do buffer.appendCodePoint(this.read());
            while (this.isAt(predicate));
            return buffer.toString();
        }
        throw new Diagnostic("Expected an identifier.", this.pointRange());
    }

    public long readInteger() {
        var buffer = new StringBuilder();
        if (this.isAt('+') || this.isAt('-'))
            buffer.appendCodePoint(this.read());
        while (this.canRead()) {
            var c = this.peek();
            if (StringReader.isDigit(c)) {
                buffer.appendCodePoint(this.read());
                continue;
            }
            break;
        }
        return Long.parseLong(buffer.toString());
    }

    public double readFloating() {
        var buffer   = new StringBuilder();
        var decimal  = false;
        var exponent = false;
        if (this.isAt('+') || this.isAt('-'))
            buffer.appendCodePoint(this.read());
        while (this.canRead()) {
            var c = this.peek();
            if (c == '.' && !exponent) {
                if (this.canRead(2)) {
                    var d = this.peek(1);
                    if (StringReader.isDigit(d)) {
                        if (decimal)
                            throw new Diagnostic("Encountered a second decimal point in a floating-point literal.", this.pointRange());
                        buffer.appendCodePoint(this.read());
                        decimal = true;
                        continue;
                    }
                    break;
                }
                break;
            }
            if (c == 'e' || c == 'E') {
                if (this.canRead(2)) {
                    var d = this.peek(1);
                    if (StringReader.isDigit(d)) {
                        if (exponent)
                            throw new Diagnostic("Encountered a second exponent in a floating-point literal.", this.pointRange());
                        buffer.appendCodePoint(this.read());
                        exponent = true;
                        continue;
                    }
                    if (d == '+' || d == '-') {
                        if (this.canRead(3)) {
                            var e = this.peek(2);
                            if (StringReader.isDigit(e)) {
                                if (exponent)
                                    throw new Diagnostic("Encountered a second exponent in a floating-point literal.", this.pointRange());
                                buffer.appendCodePoint(this.read());
                                buffer.appendCodePoint(this.read());
                                exponent = true;
                                continue;
                            }
                            break;
                        }
                        break;
                    }
                    break;
                }
                break;
            }
            if (StringReader.isDigit(c)) {
                buffer.appendCodePoint(this.read());
                continue;
            }
            break;
        }
        return Double.parseDouble(buffer.toString());
    }

    /// Returns the code point the `StringReader` is pointing to.
    ///
    /// If the code point is a carriage-return (`\r`), it is normalized to a line feed (`\n`).
    ///
    /// @since `0.1.0`
    public int peek() {
        var c = Character.codePointAt(this.source, this.position);
        if (c == '\r')
            return '\n';
        return c;
    }

    public int peek(int offset) {
        var position = this.position;
        while (offset-- > 0)
            position += Character.charCount(Character.codePointAt(this.source, position));
        var c = Character.codePointAt(this.source, position);
        if (c == '\r')
            return '\n';
        return c;
    }

    /// Read a Unicode **code point** from the source.
    ///
    /// ---
    /// Advances the position of the `StringReader` by a single code point,
    /// and returns the code point that was consumed.
    ///
    /// If a line break (either `CR`, `LF`, or `CRLF`) is consumed, it is normalized to `\n`.
    ///
    /// @since `0.1.0`
    public int read() {
        var c = Character.codePointAt(this.source, this.position);
        this.position += Character.charCount(c);
        if (c == '\n' || c == '\r') {
            if (c == '\r' && this.canRead() && Character.codePointAt(this.source, this.position) == '\n')
                this.position++;
            this.column = 1;
            this.line++;
            return '\n';
        }
        this.column++;
        return c;
    }

    public @NotNull String read(int count) {
        var buffer = new int[count];
        for (var i = 0; i < count; i++)
            buffer[i] = this.read();
        return new String(buffer, 0, count);
    }

    /// Determines if the `StringReader` is pointing to a given `point`.
    ///
    /// When pointing to the end of the source, `false` is returned.
    ///
    /// @since `0.1.0`
    public boolean isAt(int point) {
        return this.canRead() && this.peek() == point;
    }

    public boolean isAt(@NotNull IntPredicate predicate) {
        Objects.requireNonNull(predicate);
        return this.canRead() && predicate.test(this.peek());
    }

    /// Skips over the consecutive whitespace.
    ///
    /// Consumes the source as long as the code point the reader is pointing to
    /// is recognized as whitespace by [Character#isWhitespace(int)], and returns
    /// whether the source is still readable.
    ///
    /// @since `0.1.0`
    public boolean skipWhitespace() {
        while (this.isAt(Character::isWhitespace))
            this.read();
        return this.canRead();
    }

    /// Determines whether the source is still readable.
    ///
    /// @since `0.1.0`
    public boolean canRead() {
        return this.position < this.source.length();
    }

    public boolean canRead(int amount) {
        var position = this.position;
        while (--amount > 0)
            position += Character.charCount(Character.codePointAt(this.source, position));
        return position < this.source.length();
    }
}
