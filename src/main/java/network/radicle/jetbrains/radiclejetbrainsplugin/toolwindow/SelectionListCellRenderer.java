package network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow;

import com.intellij.util.ui.EmptyIcon;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.LafIconLookup;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.components.BorderLayoutPanel;

import javax.swing.ListCellRenderer;
import javax.swing.JList;
import javax.swing.JLabel;
import java.awt.Component;

public abstract class SelectionListCellRenderer<T> implements ListCellRenderer<SelectionListCellRenderer.SelectableWrapper<T>> {

    @Override
    public Component getListCellRendererComponent(JList<? extends SelectableWrapper<T>> list, SelectableWrapper<T> selectableWrapperObj,
                                                  int index, boolean isSelected, boolean cellHasFocus) {
        var icon = LafIconLookup.getIcon("checkmark", true, false);
        var panel = new BorderLayoutPanel();
        panel.setBorder(JBUI.Borders.empty(4, 0));
        panel.setForeground(UIUtil.getListBackground(isSelected, true));
        panel.setBackground(UIUtil.getListBackground(isSelected, true));
        var iconLabel = new JLabel();
        if (selectableWrapperObj.selected) {
            iconLabel.setIcon(icon);
        } else {
            iconLabel.setIcon(EmptyIcon.create(icon));
        }
        panel.addToCenter(new JLabel(getText(selectableWrapperObj.value)));
        panel.addToLeft(iconLabel);
        return  panel;
    }

    public abstract String getText(T value);

    public abstract String getPopupTitle();

    public static class SelectableWrapper<T> {
        public T value;
        public boolean selected;
        public SelectableWrapper(T value, boolean selected) {
            this.value = value;
            this.selected = selected;
        }
    }
}
