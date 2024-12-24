package network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow;

import com.intellij.collaboration.ui.SingleValueModel;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.ui.AnimatedIcon;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.components.BorderLayoutPanel;
import network.radicle.jetbrains.radiclejetbrainsplugin.icons.RadicleIcons;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.Emoji;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadAuthor;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadDetails;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.Reaction;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleNativeService;
import org.assertj.core.util.Strings;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import static network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.Utils.getHorizontalPanel;

public abstract class EmojiPanel<T> {
    private static final String FONT_NAME = "Segoe UI Emoji";
    private static final int FONT_STYLE = Font.PLAIN;
    private static final int FONT_SIZE = 14;
    private final SingleValueModel<T> model;
    private final List<Reaction> reactions;
    private final String discussionId;
    private final RadDetails radDetails;
    private final Project project;
    private final RadicleNativeService rad;
    private JBPopup reactorsPopUp;
    private JBPopup emojisPopUp;
    private JBPopupListener popupListener;
    private CountDownLatch latch;

    protected EmojiPanel(Project project, SingleValueModel<T> model, List<Reaction> reactions, String discussionId, RadDetails radDetails) {
        this.project = project;
        this.model = model;
        this.reactions = reactions;
        this.discussionId = discussionId;
        this.radDetails = radDetails;
        this.rad = project.getService(RadicleNativeService.class);
    }

    private static class EmojiRender extends SelectionListCellRenderer<Emoji> {
        @Override
        public Component getListCellRendererComponent(
                JList<? extends SelectableWrapper<Emoji>> list, SelectableWrapper<Emoji> selectableWrapperObj,
                int index, boolean isSelected, boolean cellHasFocus) {
            var jLabel = new JLabel();
            jLabel.setFont(new Font(FONT_NAME, FONT_STYLE, FONT_SIZE));
            jLabel.setText(getText(selectableWrapperObj.value));
            return jLabel;
        }

        @Override
        public String getText(Emoji emoji) {
            return emoji.unicode();
        }

        @Override
        public String getPopupTitle() {
            return "";
        }
    }

    private static class ReactorRender extends SelectionListCellRenderer<String> {
        @Override
        public String getText(String value) {
            return value;
        }

        @Override
        public String getPopupTitle() {
            return "";
        }
    }

    private static CompletableFuture<List<SelectionListCellRenderer.SelectableWrapper<Emoji>>> getEmojis() {
        return CompletableFuture.supplyAsync(() -> {
            var selectableEmojiList = new ArrayList<SelectionListCellRenderer.SelectableWrapper<Emoji>>();
            var emojiList = Emoji.loadEmojis();
            for (var emoji : emojiList) {
                selectableEmojiList.add(new SelectionListCellRenderer.SelectableWrapper<>(emoji, false));
            }
            return selectableEmojiList;
        });
    }

    private static CompletableFuture<List<SelectionListCellRenderer.SelectableWrapper<String>>> getReactors(List<RadAuthor> reactorsList) {
        return CompletableFuture.supplyAsync(() -> {
            var reactors = new ArrayList<SelectionListCellRenderer.SelectableWrapper<String>>();
            for (var reactor : reactorsList) {
                reactors.add(new SelectionListCellRenderer.SelectableWrapper<>(reactor.generateLabelText(), false));
            }
            return reactors;
        });
    }

    public JPanel getEmojiPanel() {
        var progressLabel = new JLabel(new AnimatedIcon.Default());
        progressLabel.setBorder(JBUI.Borders.empty(6, 0));
        progressLabel.setVisible(false);
        var emojiButton = new JLabel();
        emojiButton.setIcon(RadicleIcons.EMOJI);
        emojiButton.setFont(new Font(FONT_NAME, FONT_STYLE, FONT_SIZE));
        emojiButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        emojiButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                //Show the available emojis when the user click on the smiley face emoji button
                var result = new CompletableFuture<List<Emoji>>();
                var popupBuilder = new PopupBuilder(170, 40);
                emojisPopUp = popupBuilder.createHorizontalPopup(getEmojis(), new EmojiRender(), result);
                popupListener = popupBuilder.getListener();
                latch = popupBuilder.getLatch();
                emojisPopUp.showUnderneathOf(emojiButton);
                result.thenComposeAsync(selectedEmoji -> {
                    ApplicationManager.getApplication().executeOnPooledThread(() -> {
                        ApplicationManager.getApplication().invokeLater(() -> emojisPopUp.closeOk(null), ModalityState.any());
                        progressLabel.setVisible(true);
                        var res = addEmoji(selectedEmoji.get(0), discussionId);
                        progressLabel.setVisible(false);
                        var isSuccess = res != null;
                        if (isSuccess) {
                            notifyEmojiChanges(selectedEmoji.get(0).unicode(), discussionId, true);
                        }
                    });
                    return null;
                });
            }
        });
        var borderPanel = new BorderLayoutPanel();
        borderPanel.setOpaque(false);
        borderPanel.addToLeft(emojiButton);

        var horizontalPanel = getHorizontalPanel(10);
        horizontalPanel.setOpaque(false);
        horizontalPanel.add(progressLabel);
        for (var r : reactions) {
            for (var a : r.authors()) {
                if (Strings.isNullOrEmpty(a.alias)) {
                    a.alias = rad.getAlias(a.id);
                }
            }
        }
        var groupReactions = groupEmojis(reactions);
        for (var emojiUnicode : groupReactions.keySet()) {
            var reactorEmoji = new JLabel(emojiUnicode);
            reactorEmoji.setFont(new Font(FONT_NAME, FONT_STYLE, FONT_SIZE));
            var reactorsPanel = new BorderLayoutPanel();
            reactorsPanel.setOpaque(false);
            reactorsPanel.addToLeft(reactorEmoji);
            var numberOfReactors = groupReactions.get(emojiUnicode).size();
            var self = isEmojiFromCurrentUser(radDetails.nodeId, groupReactions.get(emojiUnicode));
            var numberOfReactionsLabel = new JLabel(String.valueOf(numberOfReactors));
            reactorsPanel.addToRight(numberOfReactionsLabel);
            reactorsPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            //If the user has reacted then allow him to remove his reaction
            if (self) {
                numberOfReactionsLabel.setFont(new Font(FONT_NAME, Font.BOLD, 13));
                reactorsPanel.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        ApplicationManager.getApplication().executeOnPooledThread(() -> {
                            progressLabel.setVisible(true);
                            var res = removeEmoji(emojiUnicode, discussionId);
                            var isSuccess = res != null;
                            if (isSuccess) {
                                notifyEmojiChanges(emojiUnicode, discussionId, false);
                            }
                            progressLabel.setVisible(false);
                        });
                    }
                });
            }
            var result = new CompletableFuture<List<String>>();
            reactorsPanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    //Show the reactors on emoji hover
                    var builder = new PopupBuilder();
                    reactorsPopUp = builder.createHorizontalPopup(getReactors(groupReactions.get(emojiUnicode)),
                            new ReactorRender(), result);
                    reactorsPopUp.showUnderneathOf(reactorEmoji);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    //Close the pop-up
                    reactorsPopUp.closeOk(null);
                }
            });
            horizontalPanel.add(reactorsPanel);
        }
        borderPanel.addToRight(horizontalPanel);
        borderPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        return borderPanel;
    }

    private boolean isEmojiFromCurrentUser(String currentUserId, List<RadAuthor> reactorsIds) {
        return reactorsIds.stream().anyMatch(r -> r.id.contains(currentUserId));
    }

    public JBPopupListener getPopupListener() {
        return popupListener;
    }

    public CountDownLatch getLatch() {
        return latch;
    }

    public JBPopup getEmojisPopUp() {
        return emojisPopUp;
    }

    private Map<String, List<RadAuthor>> groupEmojis(List<Reaction> myReactions) {
        return myReactions.stream().collect(Collectors.toMap(Reaction::emoji, Reaction::authors));
    }

    public abstract T addEmoji(Emoji emoji, String id);

    public abstract T removeEmoji(String emojiUnicode, String id);

    public abstract void notifyEmojiChanges(String emojiUnicode, String commentId, boolean isAdded);
}
