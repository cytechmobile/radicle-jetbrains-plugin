package network.radicle.jetbrains.radiclejetbrainsplugin.config;

import com.intellij.testFramework.LightPlatform4TestCase;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RadicleSettingsSeedNodeTest extends LightPlatform4TestCase {
    private RadicleSettingsSeedNode radicleSeetingsSeedNodeView;

    @Before
    public void before() {
        radicleSeetingsSeedNodeView = new RadicleSettingsSeedNode();
        radicleSeetingsSeedNodeView.createComponent();
        radicleSeetingsSeedNodeView.apply();
    }

    @Test
    public void testGetId() {
        assertThat(radicleSeetingsSeedNodeView.getId()).isEqualTo(RadicleSettingsSeedNode.ID);
    }

    @Test
    public void testDisplayName() {
        assertThat(radicleSeetingsSeedNodeView.getDisplayName()).isEqualTo(RadicleSettingsSeedNode.ID);
    }

    @Test
    public void testCreateComponent() {
        assertThat(radicleSeetingsSeedNodeView.createComponent()).isNotNull();
    }

    @Test
    public void testDefaultSeedNodes() {
        var loadedSeedNodes = radicleSeetingsSeedNodeView.getLoadedSeedNodes();
        assertThat(loadedSeedNodes).usingRecursiveComparison().isEqualTo(RadicleSettingsHandler.DEFAULT_SEED_NODES);
    }

    @Test
    public void testSeedNodeActions() {
        //Add seed node
        var seedNode = "192.168.1.1";
        var port = "8080";

        var newSeedNode = seedNode + RadicleSettingsHandler.RAD_SEED_SEPERATOR + port;
        var addSeedNode = radicleSeetingsSeedNodeView.new AddSeedNode();
        addSeedNode.addNode(seedNode,port);
        assertThat(radicleSeetingsSeedNodeView.isModified()).isTrue();
        radicleSeetingsSeedNodeView.apply();
        radicleSeetingsSeedNodeView = new RadicleSettingsSeedNode();
        radicleSeetingsSeedNodeView.createComponent();
        var loadedSeedNodes = radicleSeetingsSeedNodeView.getLoadedSeedNodes();
        assertThat(loadedSeedNodes).contains(newSeedNode);

        var tableIndex = loadedSeedNodes.indexOf(newSeedNode);
        //Edit seed node
        var editSeedNode = radicleSeetingsSeedNodeView.new EditSeedNode();
        var domain = "pine.radicle.garden";
        var newPort = "8085";
        editSeedNode.editNode(domain,newPort,seedNode,port,tableIndex);
        assertThat(radicleSeetingsSeedNodeView.isModified()).isTrue();
        radicleSeetingsSeedNodeView.apply();

        //Remove seed node
        radicleSeetingsSeedNodeView = new RadicleSettingsSeedNode();
        radicleSeetingsSeedNodeView.createComponent();
        var removeSeedNode = radicleSeetingsSeedNodeView.new RemoveSeedNode();
        removeSeedNode.removeNode(domain,newPort,tableIndex);
        assertThat(radicleSeetingsSeedNodeView.isModified()).isTrue();
        radicleSeetingsSeedNodeView.apply();

        radicleSeetingsSeedNodeView = new RadicleSettingsSeedNode();
        loadedSeedNodes = radicleSeetingsSeedNodeView.getLoadedSeedNodes();
        assertThat(loadedSeedNodes).usingRecursiveComparison().isEqualTo(RadicleSettingsHandler.DEFAULT_SEED_NODES);
    }



}
