package network.radicle.jetbrains.radiclejetbrainsplugin.models;

import java.util.List;

public class RadDetails {
    public String alias;
    public String did;
    public String nodeId;
    public String keyHash;

    public RadDetails(List<String> details) {
        parseDetails(details);
    }

    private void parseDetails(List<String> details) {
        if (!details.isEmpty()) {
            alias = details.get(0);
        }

        if (details.size() > 1) {
            nodeId = details.get(1);
        }

        if (details.size() > 2) {
            did = details.get(2);
        }

        if (details.size() > 3) {
            keyHash = details.get(3);
        }
    }
}
