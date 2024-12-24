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
import network.radicle.jetbrains.radiclejetbrainsplugin.models.Embed;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleNativeService;
import org.apache.tika.Tika;
import org.intellij.plugins.markdown.ui.preview.html.MarkdownUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.JEditorPane;
import javax.swing.JTextPane;
import javax.swing.event.HyperlinkEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class MarkDownEditorPaneFactory {
    public static final String IMG_WIDTH = "450px";
    public static final String EMBED_REGEX = "\\[([^]]+)\\]\\(([^)]+)\\)";
    public static final Pattern EMBED_PATTERN = Pattern.compile(EMBED_REGEX);
    public static final String IMG_REGEX = "<img\\s+[^>]*>";
    public static final Pattern IMG_PATTERN = Pattern.compile(IMG_REGEX);
    private final String radProjectId;
    private final VirtualFile file;
    private final Project project;
    private final String rawContent;
    private String content;
    private Map<String, String> embedMap;

    public MarkDownEditorPaneFactory(String content, Project project, String radProjectId, VirtualFile file) {
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
        var matcher = IMG_PATTERN.matcher(this.content);
        var imgTags = new ArrayList<String>();
        while (matcher.find()) {
            imgTags.add(matcher.group());
        }
        return imgTags;
    }

    private List<Embed> findEmbedList() {
        // Find all embeds from the content e.g [name.jpg](4f4ba)
        var matcher = EMBED_PATTERN.matcher(this.rawContent);
        var embedList = new ArrayList<Embed>();
        while (matcher.find()) {
            String filename = matcher.group(1);
            String gitObjectId = matcher.group(2);
            embedList.add(new Embed(gitObjectId, filename, null));
        }
        return embedList;
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
        var embedOids = embedList.stream().map(Embed::getOid).filter(oid -> !Strings.isNullOrEmpty(oid)).filter(oid -> !isExternalFile(oid)).toList();
        var radNative = project.getService(RadicleNativeService.class);
        var repoId = radProjectId;
        if (repoId.startsWith("rad:")) {
            repoId = repoId.substring(4);
        }
        embedMap = radNative.getEmbeds(repoId, embedOids);
        if (embedMap == null) {
            embedMap = new HashMap<>();
        }
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
        // return a data url
        if (Strings.isNullOrEmpty(mimeType)) {
            mimeType = "image/png";
        }

        var b64 = embedMap.get(objectId);
        if (Strings.isNullOrEmpty(b64)) {
            return objectId;
        }
        return "data:" + mimeType + ";base64," + b64;
    }

    private void convertMarkdownToHtml() {
        this.content = MarkdownUtil.INSTANCE.generateMarkdownHtml(file, this.content, project);
    }

    public static boolean isExternalFile(String objectId) {
        return objectId.contains("https://") || objectId.contains("http://");
    }

    public static boolean isSvg(String mimeType) {
        return mimeType.contains("svg");
    }

    public static String findMimeType(Embed embed) {
        var tika = new Tika();
        if (isExternalFile(embed.getOid())) {
            return tika.detect(embed.getOid());
        }
        return tika.detect(embed.getName());
    }

    public static String getHrefTag(String url, String fileName) {
        return "<a href=\"" + url + "\">" + fileName + "</a>";
    }

    public static String getImgTag(String url) {
        return "<div style=\"width:" + IMG_WIDTH + ";\"><img src=\"" + url + "\"/></div>";
    }

    public static String wrapHtml(String body) {
        return "<html><head><style>p, h1, h2, h3, h4, h5, h6 { margin: 0; padding: 0; }</head><body>" + body + "</body></html>";
    }

}
