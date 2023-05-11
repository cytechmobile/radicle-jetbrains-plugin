package network.radicle.jetbrains.radiclejetbrainsplugin.patches.overview;

import git4idea.GitCommit;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadPatch;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class PatchTimeLineModel extends AbstractListModel<PatchTimeLineModel.PatchTimelineItem> {
    private final List<PatchTimelineItem> list = new ArrayList<>();

    public void add(PatchTimelineItem item) {
        list.add(item);
    }

    @Override
    public int getSize() {
        return list.size();
    }

    @Override
    public PatchTimelineItem getElementAt(int index) {
        return list.get(index);
    }

    public record PatchTimelineItem(RadPatch radPatch) {

    }
}
