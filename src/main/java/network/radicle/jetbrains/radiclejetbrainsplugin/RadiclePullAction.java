package network.radicle.jetbrains.radiclejetbrainsplugin;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RadiclePullAction extends AnAction {

    private static final Logger logger = LoggerFactory.getLogger(RadiclePullAction.class);

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        logger.warn("Rad Pull");
    }
}
