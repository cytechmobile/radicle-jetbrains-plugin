package network.radicle.jetbrains.radiclejetbrainsplugin;

import com.intellij.DynamicBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class MyBundle extends DynamicBundle {
    public static final String BUNDLE = "messages.MyBundle";

    public MyBundle() {
        super(BUNDLE);
    }

    @Override
    public @NotNull @Nls String getMessage(@NotNull @NonNls String key, Object @NotNull ... params) {
        return super.getMessage(key, params);
    }

    public static @Nls @NotNull String message(@NotNull @NonNls String key, Object @NotNull ... params) {
        return new MyBundle().getMessage(key, null, params);
    }
}
