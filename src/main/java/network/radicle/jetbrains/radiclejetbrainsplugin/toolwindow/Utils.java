package network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow;


import com.intellij.collaboration.ui.codereview.list.search.ChooserPopupUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.panels.ListLayout;
import com.intellij.ui.speedSearch.NameFilteringListModel;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JComponent;
import javax.swing.JPanel;
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
