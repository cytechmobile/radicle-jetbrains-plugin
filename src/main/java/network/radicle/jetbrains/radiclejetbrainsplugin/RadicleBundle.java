package network.radicle.jetbrains.radiclejetbrainsplugin;

import com.intellij.DynamicBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class RadicleBundle extends DynamicBundle {
    public static final String BUNDLE = "messages.RadicleBundle";

    public static final RadicleBundle INSTANCE = new RadicleBundle();

    public RadicleBundle() {
        super(BUNDLE);
    }

    public static @Nls @NotNull String message(@NotNull @NonNls String key, Object @NotNull ... params) {
        return INSTANCE.messageOrDefault(key, key, params);
    }

    public static Supplier<String> lazyMessage(@NotNull @NonNls String key, Object @NotNull ... params) {
        return () -> message(key, params);
    }
}
