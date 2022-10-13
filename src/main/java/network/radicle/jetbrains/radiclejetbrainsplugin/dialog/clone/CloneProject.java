package network.radicle.jetbrains.radiclejetbrainsplugin.dialog.clone;

import java.util.List;

public interface CloneProject {
    String projectName(List<String> outputLines);
    String url();
    String directory();
}
