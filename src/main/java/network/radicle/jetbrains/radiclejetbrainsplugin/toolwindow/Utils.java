package network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow;


import com.intellij.collaboration.ui.CollaborationToolsUIUtil;
import com.intellij.collaboration.ui.codereview.list.search.ChooserPopupUtil;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.ui.popup.JBPopup;
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
import com.intellij.ui.speedSearch.NameFilteringListModel;
import com.intellij.util.ui.EmptyIcon;
import com.intellij.util.ui.InlineIconButton;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.LafIconLookup;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.WrapLayout;
import com.intellij.util.ui.components.BorderLayoutPanel;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.JLabel;
import javax.swing.ListSelectionModel;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class Utils {
    private static final Logger logger = LoggerFactory.getLogger(Utils.class);

    public static JComponent getVerticalPanel(int gap) {
        return new JPanel(ListLayout.vertical(gap, ListLayout.Alignment.CENTER, ListLayout.GrowPolicy.GROW));
    }

    public static JComponent getHorizontalPanel(int gap) {
        return new JPanel(ListLayout.horizontal(gap, ListLayout.Alignment.START, ListLayout.GrowPolicy.GROW));
    }

    public static class SelectableWrapper<T> {
        public T value;
        public boolean selected;
        public SelectableWrapper(T value, boolean selected) {
            this.value = value;
            this.selected = selected;
        }
    }

    public abstract static class  SelectionListCellRenderer<T> implements ListCellRenderer<SelectableWrapper<T>> {
        @Override
        public Component getListCellRendererComponent(JList<? extends SelectableWrapper<T>> list,
                                                      SelectableWrapper<T> selectableWrapperObj, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
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

    public static class PopupBuilder {
        public static JBPopupListener myListener;

        public static <T> JBPopup createPopup(CompletableFuture<List<Utils.SelectableWrapper<T>>> myList,
                                              Utils.SelectionListCellRenderer<T> rendered, boolean singleSelection,
                                              JBTextField inputField, CompletableFuture<List<T>> result) {
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
                            for (var i = 0; i < list.getItemsCount(); i++) {
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
            myListener = new JBPopupListener() {
                @Override
                public void beforeShown(@NotNull LightweightWindowEvent event) {
                    var popUp = event.asPopup().getContent();
                    //Start loading indicator
                    list.setPaintBusy(true);
                    if (inputField != null) {
                        var borderLayout = (BorderLayoutPanel) popUp.getComponent(0);
                        borderLayout.addToBottom(inputField);
                        inputField.addActionListener(e -> event.asPopup().closeOk(null));
                    }
                    ApplicationManager.getApplication().executeOnPooledThread(() -> {
                        try {
                            var dataList = myList.get(5, TimeUnit.SECONDS);
                            listModel.replaceAll(dataList);
                            if (dataList.size() == 0) {
                                popUp.setPreferredSize(new Dimension(100, 100));
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
            };
            return JBPopupFactory.getInstance().createComponentPopupBuilder(panel, searchField)
                    .setRequestFocus(true)
                    .setCancelOnClickOutside(true)
                    .setTitle(rendered.getPopupTitle())
                    .setResizable(true)
                    .setMovable(true)
                    .setCancelOnClickOutside(true)
                    .addListener(myListener)
                    .createPopup();
        }

        public static JBPopup createPopup(CompletableFuture<List<String>> list, CountDownLatch latch) {
            return JBPopupFactory.getInstance().createPopupChooserBuilder(new ArrayList<String>())
                    .setResizable(true)
                    .setMovable(true)
                    .setFilterAlwaysVisible(true)
                    .setRenderer(new ChooserPopupUtil.SimplePopupItemRenderer<>(s ->
                            new ChooserPopupUtil.PopupItemPresentation.Simple(s, null, null)))
                    .setNamerForFiltering(s -> s)
                    .addListener(new JBPopupListener() {
                        @Override
                        public void beforeShown(@NotNull LightweightWindowEvent event) {
                            //Get popup
                            var popUp = event.asPopup().getContent();
                            var jbList = UIUtil.findComponentOfType(popUp, JBList.class);
                            if (jbList == null) {
                                return;
                            }
                            //Find model
                            var listModel = (NameFilteringListModel<String>) jbList.getModel();
                            //Start loading indicator
                            jbList.setPaintBusy(true);
                            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                                try {
                                    //Wait for the data to be ready
                                    var isFinished = latch.await(5, TimeUnit.SECONDS);
                                    if (!isFinished) {
                                        return;
                                    }
                                    var data = list.get(5, TimeUnit.SECONDS);
                                    ApplicationManager.getApplication().invokeLater(() -> {
                                        // Update the model with the new data
                                        listModel.replaceAll(data);
                                        event.asPopup().pack(true, true);
                                    }, ModalityState.any());
                                } catch (Exception e) {
                                    logger.warn("Unable to load filters");
                                } finally {
                                    // Stop loading indicator
                                    ApplicationManager.getApplication().invokeLater(() ->
                                            jbList.setPaintBusy(false), ModalityState.any());
                                }
                            });
                        }
                    }).createPopup();
        }
    }

    public abstract static class LabeledListPanelHandle<T> {
        private final InlineIconButton editButton;
        private final JLabel progressLabel;
        private final JLabel errorIcon;
        private final JLabel titleLabel;
        public JBPopup jbPopup;
        public JBPopupListener listener;

        public LabeledListPanelHandle() {
            progressLabel = new JLabel(new AnimatedIcon.Default());
            progressLabel.setBorder(JBUI.Borders.empty(6, 0));
            errorIcon = new JLabel(AllIcons.General.Error);
            errorIcon.setBorder(JBUI.Borders.empty(6, 0));
            progressLabel.setVisible(false);
            errorIcon.setVisible(false);
            titleLabel = new JLabel();
            titleLabel.setForeground(UIUtil.getContextHelpForeground());
            titleLabel.setBorder(JBUI.Borders.empty(6, 5));
            titleLabel.setText(getLabel());
            editButton = new InlineIconButton(AllIcons.General.Inline_edit, AllIcons.General.Inline_edit_hovered);
            editButton.setBorder(JBUI.Borders.empty(6, 0));
            editButton.setActionListener(e ->
                    showEditPopup(editButton).thenComposeAsync(data -> {
                        progressLabel.setVisible(true);
                        errorIcon.setVisible(false);
                        ApplicationManager.getApplication().executeOnPooledThread(() -> {
                            var isSuccess = storeValues(data);
                            ApplicationManager.getApplication().invokeLater(() -> {
                                progressLabel.setVisible(false);
                                if (!isSuccess) {
                                    errorIcon.setVisible(true);
                                }
                            }, ModalityState.any());
                        });
                        return null;
                    }));
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
            simplePanel.add(new JLabel(getSelectedValues()));
            simplePanel.addToRight(getControlsPanel());
            var panel = new NonOpaquePanel(new WrapLayout(FlowLayout.LEADING, 0, 0));
            panel.add(simplePanel);
            return panel;
        }
        public CompletableFuture<List<T>> showEditPopup(JComponent parent) {
            var result = new CompletableFuture<List<T>>();
            jbPopup = Utils.PopupBuilder.createPopup(this.getData(), getRender(), this.isSingleSelection(), null, result);
            jbPopup.showUnderneathOf(parent);
            listener = Utils.PopupBuilder.myListener;
            return result;
        }

        public abstract String getSelectedValues();

        public abstract boolean storeValues(List<T> data);

        public abstract Utils.SelectionListCellRenderer<T> getRender();

        public abstract CompletableFuture<List<Utils.SelectableWrapper<T>>> getData();

        public abstract String getLabel();

        public abstract boolean isSingleSelection();
    }
}
