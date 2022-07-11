package network.radicle.jetbrains.radiclejetbrainsplugin.services;

import com.intellij.openapi.diagnostic.Logger;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;

public class RadicleApplicationService {

    private static final Logger log = Logger.getInstance(RadicleApplicationService.class);

    public RadicleApplicationService() {
        log.info(RadicleBundle.message("applicationService"));
    }
}
