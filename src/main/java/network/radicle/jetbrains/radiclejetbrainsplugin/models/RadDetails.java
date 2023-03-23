package network.radicle.jetbrains.radiclejetbrainsplugin.models;

import java.util.List;

public class RadDetails {
    private static final String DOUBLE_SPACE = "  ";
    private static final String SINGLE_SPACE = " ";

    public String id;
    public String nodeId;
    public String keyHash;
    public String keyFull;
    public String storageGit;
    public String storageKeys;
    public String nodeSocket;

    public RadDetails(List<String> details) {
        parseDetails(details);
    }

    /* ID             did:key:z6MkgrVBxtgFynk6AWZ9ZDM5NVnHuvWeyMJkVD1aUVtMXPug
       Node ID        z6MkgrVBxtgFynk6AWZ9ZDM5NVnHuvWeyMJkVD1aUVtMXPug
       Key (hash)     SHA256:ydAxDGTdnXu2wQ+EfWJ8xn9UMHqjH8TaovWJW6KZCsA
       Key (full)     ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAICOqmEeeO8DuIrnzyJtx/54eWyzaajQXaE0txB6W7/1X
       Storage (git)  /home/test/.radicle/storage
       Storage (keys) /home/test/.radicle/keys
       Node (socket)  /home/test/.radicle/node/radicle.sock
   */
    private void parseDetails(List<String> details) {
        if (details.size() > 0) {
            var parts = details.get(0).split(DOUBLE_SPACE);
            id = parts[parts.length - 1].trim();
        }

        if (details.size() > 1) {
            var parts = details.get(1).split(DOUBLE_SPACE);
            nodeId = parts[parts.length - 1].trim();
        }

        if (details.size() > 2) {
            var parts = details.get(2).split(DOUBLE_SPACE);
            keyHash = parts[parts.length - 1].trim();
        }

        if (details.size() > 3) {
            var parts = details.get(3).split(DOUBLE_SPACE);
            keyFull = parts[parts.length - 1].trim();
        }

        if (details.size() > 4) {
            var parts = details.get(4).split(DOUBLE_SPACE);
            storageGit = parts[parts.length - 1].trim();
        }

        if (details.size() > 5) {
            var parts = details.get(5).split(SINGLE_SPACE);
            storageKeys = parts[parts.length - 1].trim();
        }

        if (details.size() > 6) {
            var parts = details.get(6).split(DOUBLE_SPACE);
            nodeSocket = parts[parts.length - 1].trim();
        }
    }
}
