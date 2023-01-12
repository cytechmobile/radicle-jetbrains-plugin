package network.radicle.jetbrains.radiclejetbrainsplugin.config;

import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.util.ui.JBUI;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;

public class RadicleSettingsSeedNodeView implements SearchableConfigurable {

    private final RadicleSettingsHandler radicleSettingsHandler;
    public static final String ID = RadicleBundle.message("radicle");
    protected JPanel mainPanel;
    private RadicleSeedNodeDecorator seedNodeDecorator;

    public RadicleSettingsSeedNodeView() {
        this.radicleSettingsHandler = new RadicleSettingsHandler();
        seedNodeDecorator = new RadicleSeedNodeDecorator();
    }

    @Override
    public @NotNull @NonNls String getId() {
        return ID;
    }

    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return ID;
    }

    @Override
    public @Nullable JComponent createComponent() {
        initComponents();
        return mainPanel;
    }

    @Override
    public boolean isModified() {
        return !seedNodeDecorator.getLoadedSeedNodes().equals(seedNodeDecorator.getCpLoadedSeedNodes());
    }

    @Override
    public void apply() {
        radicleSettingsHandler.saveSeedNodes(seedNodeDecorator.getCpLoadedSeedNodes());
        seedNodeDecorator.loadSeedNodes();
    }

    private void initComponents() {
        initTable();
    }

    public RadicleSeedNodeDecorator getSeedNodeDecorator() {
        return seedNodeDecorator;
    }

    private void initTable() {
        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(seedNodeDecorator.initDecorator().createPanel(), BorderLayout.CENTER);
        mainPanel.setBorder(JBUI.Borders.empty(4));
    }

}
