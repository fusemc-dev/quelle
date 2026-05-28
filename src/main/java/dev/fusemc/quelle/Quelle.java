package dev.fusemc.quelle;

import com.manchickas.crayon.Crayon;
import dev.fusemc.quelle.position.CharRange;

/// ![Quelle](../../../../../../banner.png)
///
/// # Quelle
///
/// 'Quelle'
public final class Quelle {

    private Quelle() {
        throw new UnsupportedOperationException();
    }

    static void main() {
        var src = """
                {
                    "name": "Marie",
                    "age": 15,
                    "friends": [
                        {
                            "name": "Elka",
                            "age": 15,
                            "friends": []
                        }
                    ] # This is a comment
                }
                """;
        var range = CharRange.compute(src, src.indexOf('['), src.lastIndexOf(']') + 1);
        System.out.println("Hello, bar!");
        throw new Diagnostic("foo bar baz", range);
    }

    public static int width(int i) {
        if (i == 0)
            return 1;
        return (int) Math.floor(Math.log10(i)) + 1;
    }
}
