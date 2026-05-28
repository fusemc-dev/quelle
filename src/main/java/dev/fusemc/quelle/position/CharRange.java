package dev.fusemc.quelle.position;

import com.manchickas.crayon.Crayon;
import com.manchickas.crayon.Style;
import dev.fusemc.quelle.Quelle;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/// A range in a [CharSequence].
///
/// ---
/// A `CharRange` represents a range between two [CharPosition]s in a source sequence [S].
/// It is **inclusive** in the lower bound and **exclusive** in the upper one.
///
/// @since `0.1.0`
public final class CharRange<S extends CharSequence> {

    private static final @NotNull Style GUTTER = Crayon.color(0x6F737A);

    private final @NotNull S source;
    private final @NotNull CharPosition<S> begin;
    private final @NotNull CharPosition<S> end;

    public CharRange(@NotNull S source,
                     @NotNull CharPosition<S> begin,
                     @NotNull CharPosition<S> end) {
        this.source = Objects.requireNonNull(source);
        this.begin  = Objects.requireNonNull(begin);
        this.end    = Objects.requireNonNull(end);
    }

    public static <S extends CharSequence> @NotNull CharRange<S> compute(@NotNull S source, int begin, int end) {
        Objects.requireNonNull(source);
        return new CharRange<>(source,
                CharPosition.compute(source, begin),
                CharPosition.compute(source, end)
        );
    }

    /// Build a **diagnostics excerpt** for the `CharRange`.
    ///
    /// ---
    /// The built excerpt consists of **full** lines the range covers,
    /// prefixed by an aligned gutter, with the contents of the range
    /// highlighted in the given `highlight` [Style].
    ///
    /// The following might be an excerpt of a JSON source:
    ///
    /// ```java
    ///  5 |     "friends": [
    ///  6 |         {
    ///  7 |            "name": "Marie",
    ///  8 |            "age": 15
    ///  9 |         }
    /// 10 |     ]
    /// ```
    ///
    /// @since `0.1.0`
    /// @see #excerpt(Style, Style)
    public @NotNull String excerpt(@NotNull Style highlight) {
        return this.excerpt(highlight, Crayon.empty());
    }

    /// Build a **diagnostics excerpt** for the `CharRange`.
    ///
    /// ---
    /// The built excerpt consists of **full** lines the range covers,
    /// prefixed by an aligned gutter, with the contents of the range
    /// highlighted in the given `highlight` [Style].
    ///
    /// The rest of the lines outside the `CharRange` will appear
    /// as described by the `rest` parameter.
    ///
    /// The following might be an excerpt of a JSON source:
    ///
    /// ```java
    ///  5 |     "friends": [
    ///  6 |         {
    ///  7 |            "name": "Marie",
    ///  8 |            "age": 15
    ///  9 |         }
    /// 10 |     ]
    /// ```
    ///
    /// @since `0.1.0`
    public @NotNull String excerpt(@NotNull Style highlight,
                                   @NotNull Style rest) {
        Objects.requireNonNull(highlight);
        Objects.requireNonNull(rest);
        var buffer   = new StringBuilder();
        var position = this.precedingLine();
        var width    = Quelle.width(Math.max(this.begin.line, this.end.line));
        var line     = this.begin.line;
        while (position < this.end.offset) {
            var content   = new StringBuilder();
            if (this.begin.isBehind(position)) {
                highlight.end(buffer);
                buffer.append('\n');
            }
            CharRange.GUTTER.end(CharRange.GUTTER.begin(buffer)
                    .append(' ')
                    .append(String.format("%" + width + "d", line++))
                    .append(" | "));
            (this.begin.isBehind(position) ? highlight : rest).begin(buffer);
            while (position < this.source.length()) {
                var c = Character.codePointAt(this.source, position);
                if (c == '\n' || c == '\r') {
                    position++;
                    if (position < this.source.length() && c == '\r' && Character.codePointAt(this.source, position) == '\n')
                        position++;
                    break;
                }
                if (this.begin.isAt(position)) {
                    rest.end(content);
                    highlight.begin(content);
                }
                if (this.end.isAt(position)) {
                    highlight.end(content);
                    rest.begin(content);
                }
                content.appendCodePoint(c);
                position += Character.charCount(c);
            }
            buffer.append(content);
        }
        if (this.end.isAt(position)) {
            highlight.end(buffer);
            return buffer.toString();
        }
        rest.end(buffer);
        return buffer.toString();
    }

    public @NotNull String content() {
        var buffer = new StringBuilder();
        for (var position = this.begin.offset; position < this.end.offset;) {
            var c = Character.codePointAt(this.source, position);
            position += Character.charCount(c);
            buffer.appendCodePoint(c);
        }
        return buffer.toString();
    }

    @ApiStatus.Internal
    private int precedingLine() {
        var i = this.begin.offset;
        while (i > 0) {
            var c = Character.codePointBefore(this.source, i);
            if (c != '\n' && c != '\r') {
                i -= Character.charCount(c);
                continue;
            }
            return i;
        }
        return 0;
    }

    @Override
    public @NotNull String toString() {
        return String.format("(%s..%s)", this.begin, this.end);
    }
}
