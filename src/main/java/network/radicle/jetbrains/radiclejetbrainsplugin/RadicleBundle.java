package network.radicle.jetbrains.radiclejetbrainsplugin;

import com.intellij.DynamicBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class RadicleBundle extends DynamicBundle {
    public static final String BUNDLE = "messages.RadicleBundle";

    public RadicleBundle() {
        super(BUNDLE);
    }

    @Override
    public @NotNull @Nls String getMessage(@NotNull @NonNls String key, Object @NotNull ... params) {
        return super.getMessage(key, params);
    }

    public static @Nls @NotNull String message(@NotNull @NonNls String key, Object @NotNull ... params) {
        return new RadicleBundle().getMessage(key, null, params);
    }
}
