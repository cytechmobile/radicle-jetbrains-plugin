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

public class MarkDownEditorPaneFactory {
    public static final String IMG_WIDTH = "450px";
    private final RadicleProjectSettings settings;
    private final String radProjectId;
    private final VirtualFile file;
    private final Project project;
    private final String rawContent;
    private String content;

    public MarkDownEditorPaneFactory(String content, Project project, String radProjectId, VirtualFile file) {
        this.settings = new RadicleProjectSettingsHandler(project).loadSettings();
        this.content = content;
        this.radProjectId = radProjectId;
        this.file = file;
        this.project = project;
        this.rawContent = content;
        this.convertMarkdownToHtml();
        this.replaceHtmlTags();
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
                //Open file in browser
                if (e.getURL() != null) {
                    BrowserUtil.browse(e.getURL());
                }
            }
        });
        return textPane;
    }

    public String getRawContent() {
        return this.rawContent;
    }

    private List<String> getAllImgHtmlTags() {
        var imgTags = new ArrayList<String>();
        var imgPattern = "<img\\s+[^>]*>";
        var pattern = Pattern.compile(imgPattern);
        var matcher = pattern.matcher(this.content);
        while (matcher.find()) {
            imgTags.add(matcher.group());
        }
        return imgTags;
    }

    private List<Embed> findEmbedList() {
        // Find all embeds from the content e.g [name.jpg](4f4ba)
        String regex = "\\[([^]]+)\\]\\(([^)]+)\\)";
        var pattern = Pattern.compile(regex);
        var matcher = pattern.matcher(this.rawContent);
        var embedList = new ArrayList<Embed>();
        while (matcher.find()) {
            String filename = matcher.group(1);
            String gitObjectId = matcher.group(2);
            embedList.add(new Embed(gitObjectId, filename, null));
        }
        return embedList;
    }

    private boolean isExternalFile(String objectId) {
        return objectId.contains("https://") || objectId.contains("http://");
    }

    private boolean isSvg(String mimeType) {
        return mimeType.contains("svg");
    }

    private String findMimeType(Embed embed) {
        var tika = new Tika();
        if (isExternalFile(embed.getOid())) {
            return tika.detect(embed.getOid());
        }
        return tika.detect(embed.getName());
    }

    private void replaceHtmlTags() {
        var embedList = findEmbedList();
        if (embedList.isEmpty()) {
            return;
        }
        var imgTags = getAllImgHtmlTags();
        if (imgTags.isEmpty()) {
            return;
        }
        var map = new HashMap<String, String>();
        for (var embed : embedList) {
            var objectId = embed.getOid();
            if (Strings.isNullOrEmpty(objectId)) {
                continue;
            }
            var mime = findMimeType(embed);
            var imgTag = imgTags.stream().filter(tag -> tag.contains(objectId)).findFirst();
            if (imgTag.isPresent()) {
                String newTag;
                var oldTag = imgTag.get();
                //Remove old tag from the list because there is a case that the user upload the same file
                imgTags.remove(oldTag);
                if (!mime.contains("image") || isSvg(mime)) {
                    newTag = getHrefTag(getBlobUrl(objectId, mime), embed.getName());
                } else {
                    newTag = getImgTag(getBlobUrl(objectId, mime));
                }
                var newHtmlTag = oldTag.replace(oldTag, newTag);
                map.put(oldTag, newHtmlTag);
            }
        }
        //Replace old tags with the new ones
        for (var oldTag : map.keySet()) {
            var newTag = map.get(oldTag);
            this.content = this.content.replace(oldTag, newTag);
        }
    }

    private String getBlobUrl(String objectId, String mimeType) {
        if (isExternalFile(objectId)) {
            return objectId;
        }
        var url = settings.getSeedNode() + "/raw/" + radProjectId + "/blobs/" + objectId;
        if (!Strings.isNullOrEmpty(mimeType)) {
            //In order to know the browser what type of file is and open it
            url += "?mime=" + mimeType;
        }
        return url;
    }

    private String getHrefTag(String url, String fileName) {
        return "<a href=\"" + url + "\">" + fileName + "</a>";
    }

    private String getImgTag(String url) {
        return "<div style=\"width:" + IMG_WIDTH + ";\"><img src=\"" + url + "\"/></div>";
    }

    public static String wrapHtml(String body) {
        return "<html><head><style>p, h1, h2, h3, h4, h5, h6 { margin: 0; padding: 0; }</head><body>" + body + "</body></html>";
    }

    private void convertMarkdownToHtml() {
        this.content = MarkdownUtil.INSTANCE.generateMarkdownHtml(file, this.content, project);
    }

}
