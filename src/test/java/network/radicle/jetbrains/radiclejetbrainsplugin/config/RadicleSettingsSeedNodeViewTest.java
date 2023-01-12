package network.radicle.jetbrains.radiclejetbrainsplugin.config;

import com.intellij.testFramework.LightPlatform4TestCase;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.SeedNode;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RadicleSettingsSeedNodeViewTest extends LightPlatform4TestCase {
    private RadicleSettingsSeedNodeView radicleSeetingsSeedNodeView;

    @Before
    public void before() {
        radicleSeetingsSeedNodeView = new RadicleSettingsSeedNodeView();
        radicleSeetingsSeedNodeView.createComponent();
        radicleSeetingsSeedNodeView.apply();
    }

    @Test
    public void testGetId() {
        assertThat(radicleSeetingsSeedNodeView.getId()).isEqualTo(RadicleSettingsSeedNodeView.ID);
    }

    @Test
    public void testDisplayName() {
        assertThat(radicleSeetingsSeedNodeView.getDisplayName()).isEqualTo(RadicleSettingsSeedNodeView.ID);
    }

    @Test
    public void testCreateComponent() {
        assertThat(radicleSeetingsSeedNodeView.createComponent()).isNotNull();
    }

    @Test
    public void testDefaultSeedNodes() {
        var loadedSeedNodes = radicleSeetingsSeedNodeView.getSeedNodeDecorator().getLoadedSeedNodes();
        assertThat(loadedSeedNodes).usingRecursiveComparison().isEqualTo(RadicleSettingsHandler.DEFAULT_SEED_NODES);
    }

    @Test
    public void testSeedNodeActions() {
        //Add seed node
        var seedNode = "192.168.1.1";
        var port = "8080";
        var newSeedNode = new SeedNode(seedNode, port);
        var addSeedNode = radicleSeetingsSeedNodeView.getSeedNodeDecorator();
        addSeedNode.new AddSeedNode().addNode(seedNode, port);
        assertThat(radicleSeetingsSeedNodeView.isModified()).isTrue();
        radicleSeetingsSeedNodeView.apply();
        radicleSeetingsSeedNodeView = new RadicleSettingsSeedNodeView();
        radicleSeetingsSeedNodeView.createComponent();
        var loadedSeedNodes = radicleSeetingsSeedNodeView.getSeedNodeDecorator().getLoadedSeedNodes();
        assertThat(loadedSeedNodes).contains(newSeedNode);

        //Edit seed node
        var tableIndex = loadedSeedNodes.indexOf(newSeedNode);
        var editSeedNode = radicleSeetingsSeedNodeView.getSeedNodeDecorator().new EditSeedNode();
        var domain = "pine.radicle.garden";
        var newPort = "8085";
        editSeedNode.editNode(domain, newPort, seedNode, port, tableIndex);
        assertThat(radicleSeetingsSeedNodeView.isModified()).isTrue();
        radicleSeetingsSeedNodeView.apply();
        loadedSeedNodes = radicleSeetingsSeedNodeView.getSeedNodeDecorator().getLoadedSeedNodes();
        assertThat(loadedSeedNodes).contains(new SeedNode(domain, newPort));
        assertThat(loadedSeedNodes.size()).isEqualTo(4);

        //Remove seed node
        radicleSeetingsSeedNodeView = new RadicleSettingsSeedNodeView();
        radicleSeetingsSeedNodeView.createComponent();
        var removeSeedNode = radicleSeetingsSeedNodeView.getSeedNodeDecorator().new RemoveSeedNode();
        removeSeedNode.removeNode(domain, newPort, tableIndex);
        assertThat(radicleSeetingsSeedNodeView.isModified()).isTrue();
        radicleSeetingsSeedNodeView.apply();

        radicleSeetingsSeedNodeView = new RadicleSettingsSeedNodeView();
        loadedSeedNodes = radicleSeetingsSeedNodeView.getSeedNodeDecorator().getLoadedSeedNodes();
        assertThat(loadedSeedNodes).usingRecursiveComparison().isEqualTo(RadicleSettingsHandler.DEFAULT_SEED_NODES);
    }

}
