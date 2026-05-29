package dev.fusemc.quelle;

import com.manchickas.crayon.Crayon;
import com.manchickas.crayon.Style;
import dev.fusemc.quelle.position.CharRange;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/// An exception associated with a [CharRange].
///
/// ---
/// Represents an exception that occurred at a certain position in a source sequence.
/// You are encouraged to _extend this class_ for other parsing errors, such that it's
/// possible to `catch` all diagnostics equally:
///
/// ```java
/// try {
///     var example = Example.parse("foo");
///     var other   = Other.parse("bar");
/// } catch (Diagnostic e) {
///     var range = e.range();
///     System.err.println(range.excerpt(Crayon.red()));
///     System.err.println(e.getMessage());
/// }
/// ```
///
/// @since `0.1.0`
public class Diagnostic extends RuntimeException {

    private static final @NotNull Style RANGE = Crayon.color(0x6F737A);
    private final @NotNull CharRange<?> range;

    public Diagnostic(@NotNull String message,
                      @NotNull CharRange<?> range) {
        super(Objects.requireNonNull(message));
        this.range = Objects.requireNonNull(range);
    }

    /// Compute the message of the diagnostic.
    ///
    /// ---
    /// The built message consists of the associated [CharRange], formatted in gray (`#6F737A`),
    /// followed by the message passed to the constructor:
    ///
    /// ```
    /// (1:1) foo bar baz
    /// ```
    ///
    /// @since `0.1.0`
    @Override
    public @NotNull String getMessage() {
        return Diagnostic.RANGE.wrap(this.range.toString()) + ' ' + super.getMessage();
    }

    /// Return the [CharRange] associated with the diagnostic.
    ///
    /// ---
    /// @since `0.1.0`
    public @NotNull CharRange<?> range() {
        return this.range;
    }
}
