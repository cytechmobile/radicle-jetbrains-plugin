package network.radicle.jetbrains.radiclejetbrainsplugin.models;

import java.util.ArrayList;
import java.util.List;

public class ThreadModel {
    private final int line;
    private final List<RadDiscussion> radDiscussion = new ArrayList<>();

    public ThreadModel(int line) {
        this.line = line;
    }

    public ThreadModel(int line, RadDiscussion discussion) {
        this.line = line;
        addRadDiscussion(discussion);
    }

    public int getLine() {
        return line;
    }

    public List<RadDiscussion> getRadDiscussion() {
        return radDiscussion;
    }

    public void addRadDiscussion(RadDiscussion discussion) {
        this.radDiscussion.add(discussion);
    }
}
