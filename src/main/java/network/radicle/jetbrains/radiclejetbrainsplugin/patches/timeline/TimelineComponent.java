package network.radicle.jetbrains.radiclejetbrainsplugin.patches.timeline;

import com.intellij.collaboration.ui.CollaborationToolsUIUtil;
import com.intellij.collaboration.ui.codereview.BaseHtmlEditorPane;
import com.intellij.collaboration.ui.codereview.CodeReviewChatItemUIUtil;
import com.intellij.collaboration.ui.codereview.CodeReviewTimelineUIUtil;
import com.intellij.collaboration.ui.codereview.CodeReviewTitleUIUtil;
import com.intellij.collaboration.ui.util.MigLayoutUtilKt;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.panels.ListLayout;
import com.intellij.ui.components.panels.Wrapper;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import net.miginfocom.layout.AC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadPatch;
import javax.swing.JComponent;
import javax.swing.JPanel;

public class TimelineComponent {
    private final TimelineComponentFactory componentsFactory;
    private final RadPatch radPatch;
    private BaseHtmlEditorPane headerTitle;

    public TimelineComponent(RadPatch radPatch) {
        this.radPatch = radPatch;
        componentsFactory = new TimelineComponentFactory(radPatch);
    }

    public JComponent create() {
        var header = getHeader();
        var descriptionWrapper = new Wrapper();
        descriptionWrapper.setOpaque(false);
        descriptionWrapper.setContent(componentsFactory.createDescSection());

        var timelinePanel = new JPanel(ListLayout.vertical(0, ListLayout.Alignment.CENTER, ListLayout.GrowPolicy.GROW));
        timelinePanel.setBorder(JBUI.Borders.empty(CodeReviewTimelineUIUtil.VERT_PADDING, 0));

        timelinePanel.setOpaque(false);
        timelinePanel.add(header);
        timelinePanel.add(descriptionWrapper);
        timelinePanel.add(componentsFactory.createRevisionSection());

        var mainPanel = new Wrapper();
        var scrollPanel = ScrollPaneFactory.createScrollPane(timelinePanel, true);
        scrollPanel.setOpaque(false);

        mainPanel.setContent(scrollPanel);
        return mainPanel;
    }

    private JComponent getHeader() {
        headerTitle = new BaseHtmlEditorPane();
        headerTitle.setFont(JBFont.h2().asBold());
        var title = CodeReviewTitleUIUtil.INSTANCE.createTitleText(radPatch.title, radPatch.id, "", "");
        headerTitle.setBody(title);

        var panel = new JPanel(new MigLayout(MigLayoutUtilKt.emptyBorders(new LC()).fillX().hideMode(3), new AC().gap("push")));
        CollaborationToolsUIUtil.INSTANCE.wrapWithLimitedSize(panel, CodeReviewChatItemUIUtil.INSTANCE.getTEXT_CONTENT_WIDTH(), 0);
        panel.setBorder(JBUI.Borders.empty(CodeReviewTimelineUIUtil.HEADER_VERT_PADDING, CodeReviewTimelineUIUtil.ITEM_HOR_PADDING));
        panel.setOpaque(false);
        panel.add(headerTitle);
        return panel;
    }

    public BaseHtmlEditorPane getHeaderTitle() {
        return headerTitle;
    }

    public TimelineComponentFactory getComponentsFactory() {
        return componentsFactory;
    }
}
