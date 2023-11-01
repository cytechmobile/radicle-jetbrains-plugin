package network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow;

import com.google.common.base.Strings;
import com.intellij.collaboration.ui.HtmlEditorPaneUtil;
import com.intellij.ide.BrowserUtil;
import com.intellij.ide.ui.AntialiasingType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.HyperlinkAdapter;
import com.intellij.util.ui.ExtendableHTMLViewFactory;
import com.intellij.util.ui.GraphicsUtil;
import com.intellij.util.ui.HTMLEditorKitBuilder;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBInsets;
import com.intellij.util.ui.StyleSheetUtil;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleProjectSettings;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleProjectSettingsHandler;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.Embed;
import org.apache.tika.Tika;
import org.intellij.plugins.markdown.ui.preview.html.MarkdownUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.JTextPane;
import javax.swing.JEditorPane;
import javax.swing.event.HyperlinkEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

public class MarkDownEditorPane {
    public static final String SPLIT_SYMBOL = ":";
    private final RadicleProjectSettings settings;
    private final List<Embed> embedList;
    private final String radProjectId;
    private final VirtualFile file;
    private final Project project;
    private final String rawContent;
    private String content;

    public MarkDownEditorPane(String content, Project project, List<Embed> embedList, String radProjectId, VirtualFile file) {
        this.settings = new RadicleProjectSettingsHandler(project).loadSettings();
        this.content = content;
        this.embedList = embedList;
        this.radProjectId = radProjectId;
        this.file = file;
        this.project = project;
        this.rawContent = content;
        this.convertMarkdownToHtml();
        this.replaceHtmlTags();
    }

    private void convertMarkdownToHtml() {
        this.content = MarkdownUtil.INSTANCE.generateMarkdownHtml(file, this.content, project);
    }

    private List<String> getAllImgHtmlTags() {
        //Find all the imgs tag inside the text
        var imgTags = new ArrayList<String>();
        var imgPattern = "<img\\s+[^>]*>";
        var pattern = Pattern.compile(imgPattern);
        var matcher = pattern.matcher(this.content);
        while (matcher.find()) {
            imgTags.add(matcher.group());
        }
        return imgTags;
    }

    private void replaceHtmlTags() {
        if (embedList.isEmpty()) {
            return;
        }
        var imgTags = getAllImgHtmlTags();
        if (imgTags.isEmpty()) {
            return;
        }
        var map = new HashMap<String, String>();
        var tika = new Tika();
        for (var embed : embedList) {
            var objectId = getObjectId(embed.getContent());
            if (Strings.isNullOrEmpty(objectId)) {
                continue;
            }
            var mime = tika.detect(embed.getName());
            var imgTag = imgTags.stream().filter(tag -> tag.contains(objectId)).findFirst();
            if (imgTag.isPresent()) {
                String newTag = "";
                var oldTag = imgTag.get();
                if (!mime.contains("image")) {
                     newTag = getHrefTag(getBlobUrl(objectId, mime), embed.getName());
                } else {
                     newTag = getImgTag(getBlobUrl(objectId, mime));
                }
                var newHtmlTag = oldTag.replace(oldTag, newTag);
                map.put(oldTag, newHtmlTag);
            }
        }
        //Replace all old tags with the new ones
        for (var oldTag : map.keySet()) {
            var newTag = map.get(oldTag);
            this.content = this.content.replace(oldTag, newTag);
        }
    }

    public JEditorPane htmlEditorPane() {
        var textPane = new JTextPane();
        var editorKit = new HTMLEditorKitBuilder();
        editorKit.withViewFactoryExtensions(ExtendableHTMLViewFactory.Extensions.WORD_WRAP, HtmlEditorPaneUtil.INSTANCE.getCONTENT_TOOLTIP(),
                HtmlEditorPaneUtil.INSTANCE.getINLINE_ICON_EXTENSION(),
                HtmlEditorPaneUtil.INSTANCE.getIMAGES_EXTENSION());
        editorKit.withStyleSheet(StyleSheetUtil.getDefaultStyleSheet());
        textPane.setEditable(false);
        textPane.setOpaque(false);
        textPane.setFont(JBFont.h4().asPlain());
        textPane.setEditorKit(editorKit.build());
        textPane.setMargin(JBInsets.emptyInsets());
        GraphicsUtil.setAntialiasingType(textPane, AntialiasingType.getAAHintForSwingComponent());
        textPane.setText(wrapHtml(this.content));
        textPane.addHyperlinkListener(new HyperlinkAdapter() {
            @Override
            protected void hyperlinkActivated(@NotNull HyperlinkEvent e) {
                //Open browser
                BrowserUtil.browse(e.getURL());
            }
        });
        return textPane;
    }

    private String getObjectId(String myContent) {
        var parts = myContent.split(SPLIT_SYMBOL);
        if (parts.length > 1) {
            return parts[1];
        }
        return null;
    }

    private String getBlobUrl(String objectId, String mimeType) {
        var url = settings.getSeedNode() + "/raw/" + radProjectId + "/blobs/" + objectId;
        if (!Strings.isNullOrEmpty(mimeType)) {
            //In order to know the browser what type of file is and open it
            url += "?mime=" + mimeType;
        }
        return url;
    }

    public String getRawContent() {
        return this.rawContent;
    }

    private String getHrefTag(String url, String fileName) {
        return "<a href=\"" + url + "\">" + fileName + "</a>";
    }

    private String getImgTag(String url) {
        return "<img src=\"" + url + "\"/>";
    }

    private static String wrapHtml(String body) {
        return "<html><head></head><body" + body + "</body></html>";
    }
}
