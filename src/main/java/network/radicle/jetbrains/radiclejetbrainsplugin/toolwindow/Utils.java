package network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow;


import com.intellij.collaboration.ui.CollaborationToolsUIUtil;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.ui.AnimatedIcon;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.ListUtil;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.SearchTextField;
import com.intellij.ui.SideBorder;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.components.panels.HorizontalLayout;
import com.intellij.ui.components.panels.ListLayout;
import com.intellij.ui.components.panels.NonOpaquePanel;
import com.intellij.util.ui.EmptyIcon;
import com.intellij.util.ui.InlineIconButton;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.LafIconLookup;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.WrapLayout;
import com.intellij.util.ui.components.BorderLayoutPanel;
import git4idea.GitCommit;
import git4idea.history.GitHistoryUtils;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadPatch;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectService;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;


public class Utils {
    private static final Logger logger = LoggerFactory.getLogger(Utils.class);
    private final RadPatch patch;

    public Utils(RadPatch patch) {
        this.patch = patch;
    }

    public Map<String, List<GitCommit>> calculateCommits() {
        var revisions = new HashMap<String, List<GitCommit>>();
        var success = fetchCommits();
        if (!success) {
            return null;
        }
        try {
            for (var rev : patch.revisions) {
                var patchCommits = GitHistoryUtils.history(patch.repo.getProject(),
                        patch.repo.getRoot(), rev.base() + "..." + rev.oid());
                revisions.put(rev.id(), patchCommits);
            }
            return revisions;
        } catch (Exception e) {
            logger.warn("error calculating patch commits for patch: {}", patch, e);
            return null;
        }
    }

    private boolean fetchCommits() {
        var service = patch.repo.getProject().getService(RadicleProjectService.class);
        var output = service.fetchPeerChanges(patch);
        return RadAction.isSuccess(output);
    }


    public static JComponent getVerticalPanel(int gap) {
        return new JPanel(ListLayout.vertical(gap, ListLayout.Alignment.CENTER, ListLayout.GrowPolicy.GROW));
    }

    public static JComponent getHorizontalPanel(int gap) {
        return new JPanel(ListLayout.horizontal(gap, ListLayout.Alignment.START, ListLayout.GrowPolicy.GROW));
    }

    public static class SelectableWrapper<T> {
        public T value;
        public boolean selected = false;
        public SelectableWrapper(T value, boolean selected) {
            this.value = value;
            this.selected = selected;
        }
    }

    public abstract static class  SelectionListCellRenderer<T> implements ListCellRenderer<SelectableWrapper<T>> {
        @Override
        public Component getListCellRendererComponent(JList<? extends SelectableWrapper<T>> list, SelectableWrapper<T> selectableWrapperObj, int index, boolean isSelected, boolean cellHasFocus) {
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
    }
    public static <T> CompletableFuture<List<T>> createPopup(JComponent parent,
                                                                                CompletableFuture<List<Utils.SelectableWrapper<T>>> myList,
                                                                                Utils.SelectionListCellRenderer<T> rendered, boolean singleSelection, JBTextField inputField) {
        var listModel = new CollectionListModel<SelectableWrapper<T>>();
        var list = new JBList<>(listModel);
        list.setVisibleRowCount(7);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(rendered);
        var scrollPane = ScrollPaneFactory.createScrollPane(list, true);
        scrollPane.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        scrollPane.setFocusable(false);
        var searchField = new SearchTextField(false);
        searchField.setBorder(IdeBorderFactory.createBorder(SideBorder.BOTTOM));
        UIUtil.setBackgroundRecursively(searchField, UIUtil.getListBackground());
        searchField.getTextEditor().setBorder(JBUI.Borders.empty());
        CollaborationToolsUIUtil.INSTANCE.attachSearch(list, searchField,
                selectableWrapper -> rendered.getText(selectableWrapper.value));
        var panel = JBUI.Panels.simplePanel(scrollPane).addToTop(searchField.getTextEditor());
        ListUtil.installAutoSelectOnMouseMove(list);
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (UIUtil.isActionClick(e, MouseEvent.MOUSE_RELEASED) && !UIUtil.isSelectionButtonDown(e) && !e.isConsumed()) {
                    if (singleSelection) {
                        for (var i = 0; i < list.getItemsCount(); i ++) {
                            var item = list.getModel().getElementAt(i);
                            item.selected = false;
                        }
                    }
                    for (var item : list.getSelectedValuesList()) {
                        item.selected = !item.selected;
                    }
                    list.repaint();
                }
            }
        });
        var result = new CompletableFuture<List<T>>();
        JBPopupFactory.getInstance().createComponentPopupBuilder(panel, searchField)
                .setRequestFocus(true)
                .setCancelOnClickOutside(true)
                .setTitle(rendered.getPopupTitle())
                .setResizable(true)
                .setMovable(true)
                .setCancelOnClickOutside(true)
                .addListener(new JBPopupListener() {
                    @Override
                    public void beforeShown(@NotNull LightweightWindowEvent event) {
                        var popUp = event.asPopup().getContent();
                        //Start loading indicator
                        list.setPaintBusy(true);
                        var borderLayout = (BorderLayoutPanel) popUp.getComponent(0);
                        if (inputField != null) {
                            borderLayout.addToBottom(inputField);
                            inputField.addActionListener(e -> event.asPopup().closeOk(null));
                        }
                        ApplicationManager.getApplication().executeOnPooledThread(() -> {
                            try {
                                var dataList = myList.get(5, TimeUnit.SECONDS);
                                listModel.replaceAll(dataList);
                                if (dataList.size() == 0) {
                                    popUp.setPreferredSize(new Dimension(100,100));
                                }
                                event.asPopup().pack(true, true);
                            } catch (Exception e) {
                                logger.warn("Unable to load popup data", e);
                            } finally {
                                // Stop loading indicator
                                ApplicationManager.getApplication().invokeLater(() ->
                                        list.setPaintBusy(false), ModalityState.any());
                            }
                        });
                    }
                    @Override
                    public void onClosed(@NotNull LightweightWindowEvent event) {
                        var selected = listModel.getItems().stream().filter(item -> item.selected).toList();
                        var data = selected.stream().map(el -> el.value).toList();
                        result.complete(data);
                    }
                }).createPopup()
                .showUnderneathOf(parent);

        return result;
    }

    public abstract static class LabeledListPanelHandle<T> {
        private final InlineIconButton editButton;
        private final JLabel progressLabel;
        private final JLabel errorIcon;
        private final JLabel titleLabel;
        private final JLabel values;

        public LabeledListPanelHandle() {
            titleLabel = new JLabel();
            values = new JLabel();
            titleLabel.setForeground(UIUtil.getContextHelpForeground());
            titleLabel.setBorder(JBUI.Borders.empty(6, 5));
            titleLabel.setText(getLabel());
            editButton = new InlineIconButton(AllIcons.General.Inline_edit, AllIcons.General.Inline_edit_hovered);
            editButton.setBorder(JBUI.Borders.empty(6, 0));
            editButton.setActionListener(e -> {
                    showEditPopup(editButton).thenComposeAsync(new Function() {
                        @Override
                        public Object apply(Object o) {
                            progressLabel.setVisible(true);
                            errorIcon.setVisible(false);
                            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                                var isSuccess = storeValues((List<T>) o);
                                ApplicationManager.getApplication().invokeLater(() -> {
                                    progressLabel.setVisible(false);
                                    // Show error if the request failed
                                    if (!isSuccess) {
                                        errorIcon.setVisible(true);
                                    }
                                }, ModalityState.any());
                            });
                            return null;
                        }
                    });
            });
            progressLabel = new JLabel(new AnimatedIcon.Default());
            progressLabel.setBorder(JBUI.Borders.empty(6, 0));
            errorIcon = new JLabel(AllIcons.General.Error);
            errorIcon.setBorder(JBUI.Borders.empty(6, 0));
            progressLabel.setVisible(false);
            errorIcon.setVisible(false);
        }

        private JPanel getControlsPanel() {
            var controlsPanel = new JPanel(new HorizontalLayout(4));
            controlsPanel.setOpaque(false);
            controlsPanel.add(editButton);
            controlsPanel.add(progressLabel);
            controlsPanel.add(errorIcon);
            return controlsPanel;
        }

        public JLabel getTitleLabel() {
            return titleLabel;
        }

        public JComponent getPanel() {
            var simplePanel = JBUI.Panels.simplePanel();
            simplePanel.setOpaque(false);
            values.setText(getSelectedValues());
            simplePanel.add(values);
            simplePanel.addToRight(getControlsPanel());
            var panel = new NonOpaquePanel(new WrapLayout(FlowLayout.LEADING,0,0));
            panel.add(simplePanel);
            return panel;
        }

        public abstract String getSelectedValues();

        public abstract boolean storeValues(List<T> data);

        public abstract CompletableFuture<List<T>> showEditPopup(JComponent parent);

        public abstract String getLabel();
    }
}
