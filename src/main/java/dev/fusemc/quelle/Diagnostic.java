package dev.fusemc.quelle;

import com.manchickas.crayon.Crayon;
import com.manchickas.crayon.Style;
import dev.fusemc.quelle.position.CharRange;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class Diagnostic extends RuntimeException {

    private static final @NotNull Style RANGE = Crayon.color(0x6F737A);
    private final @NotNull CharRange<?> range;

    public Diagnostic(@NotNull String message,
                      @NotNull CharRange<?> range) {
        super(Objects.requireNonNull(message));
        this.range = Objects.requireNonNull(range);
    }

    @Override
    public @NotNull String getMessage() {
        return Diagnostic.RANGE.wrap(this.range.toString()) + ' ' + super.getMessage();
    }

    public @NotNull CharRange<?> range() {
        return this.range;
    }
}
