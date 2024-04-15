package network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow;

import com.intellij.collaboration.ui.CollaborationToolsUIUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.ListUtil;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.SearchTextField;
import com.intellij.ui.SideBorder;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.speedSearch.NameFilteringListModel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.components.BorderLayoutPanel;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.JList;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class PopupBuilder {
    private static final Logger logger = LoggerFactory.getLogger(Utils.class);
    private JBPopupListener myListener;
    private CountDownLatch latch;
    private Integer width;
    private Integer height;

    public PopupBuilder(Integer width, Integer height) {
        this.width = width;
        this.height = height;
    }

    public PopupBuilder() {
    }

    public JBPopup createHorizontalPopup(JPanel component, boolean resizable, boolean movable) {
        return JBPopupFactory.getInstance().createComponentPopupBuilder(component, null)
                .setRequestFocus(true)
                .setCancelOnClickOutside(true)
                .setResizable(resizable)
                .setMovable(movable)
                .setCancelOnClickOutside(true)
                .createPopup();
    }

    public <T> JBPopup createHorizontalPopup(CompletableFuture<List<SelectionListCellRenderer.SelectableWrapper<T>>> myList,
                                             SelectionListCellRenderer<T> rendered, CompletableFuture<List<T>> result) {
        latch = new CountDownLatch(1);
        var listModel = new CollectionListModel<SelectionListCellRenderer.SelectableWrapper<T>>();
        var list = new JBList<>(listModel);
        list.setVisibleRowCount(7);
        list.setCellRenderer(rendered);
        var scrollPane = new JBScrollPane(list);
        scrollPane.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        scrollPane.setFocusable(false);
        var panel = JBUI.Panels.simplePanel(scrollPane);
        ListUtil.installAutoSelectOnMouseMove(list);
        list.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        list.setVisibleRowCount(-1);
        list.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                var selectedItem = list.getSelectedValue();
                result.complete(List.of(selectedItem.value));
            }
        });
        myListener = new JBPopupListener() {
            @Override
            public void beforeShown(@NotNull LightweightWindowEvent event) {
                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    try {
                        var dataList = myList.get(5, TimeUnit.SECONDS);
                        listModel.replaceAll(dataList);
                        if (width != null && height != null) {
                            event.asPopup().getContent().setPreferredSize(new Dimension(width, height));
                        } else {
                            event.asPopup().pack(true, true);
                        }
                        latch.countDown();
                    } catch (Exception e) {
                        logger.warn("Unable to load popup data", e);
                    } finally {
                        // Stop loading indicator
                        ApplicationManager.getApplication().invokeLater(() ->
                                list.setPaintBusy(false), ModalityState.any());
                    }
                });
            }
        };
        return JBPopupFactory.getInstance().createComponentPopupBuilder(panel, null)
                .setRequestFocus(true)
                .setCancelOnClickOutside(true)
                .setResizable(true)
                .setMovable(true)
                .setTitle(rendered.getPopupTitle())
                .setCancelOnClickOutside(true)
                .addListener(myListener)
                .createPopup();
    }

    public <T> JBPopup createPopup(CompletableFuture<List<SelectionListCellRenderer.SelectableWrapper<T>>> myList,
                                          SelectionListCellRenderer<T> rendered, boolean singleSelection,
                                          JBTextField inputField, CompletableFuture<List<T>> result) {
        latch = new CountDownLatch(1);
        var listModel = new CollectionListModel<SelectionListCellRenderer.SelectableWrapper<T>>();
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
                        var dataList = myList.get(10, TimeUnit.SECONDS);
                        listModel.replaceAll(dataList);
                        if (dataList.isEmpty()) {
                            popUp.setPreferredSize(new Dimension(100, 100));
                        }
                        event.asPopup().pack(true, true);
                        latch.countDown();
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

    public JBPopup createPopup(CompletableFuture<List<String>> list, CountDownLatch myLatch) {
        return JBPopupFactory.getInstance().createPopupChooserBuilder(new ArrayList<String>())
                .setResizable(true)
                .setMovable(true)
                .setFilterAlwaysVisible(true)
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
                                var isFinished = myLatch.await(5, TimeUnit.SECONDS);
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

    public JBPopupListener getListener() {
        return myListener;
    }

    public CountDownLatch getLatch() {
        return latch;
    }
}
