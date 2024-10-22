package network.radicle.jetbrains.radiclejetbrainsplugin.remoterobot.utils;

import com.intellij.remoterobot.RemoteRobot;
import com.intellij.remoterobot.fixtures.ContainerFixture;
import com.intellij.remoterobot.search.locators.Locators;
import network.radicle.jetbrains.radiclejetbrainsplugin.remoterobot.steps.ReusableSteps;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import org.assertj.core.util.Strings;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class RemoteRobotExtension implements AfterTestExecutionCallback, ParameterResolver {
    private static final Logger logger = LoggerFactory.getLogger(RemoteRobotExtension.class);
    private final String url;
    private final RemoteRobot remoteRobot;
    private final OkHttpClient client;

    public RemoteRobotExtension() {
        var rrurl = System.getProperty("remote-robot-url");
        if (Strings.isNullOrEmpty(rrurl)) {
            rrurl = "http://127.0.0.1:8082";
        }
        this.url = rrurl;
        var okc = new OkHttpClient.Builder();
        if ("enable".equals(System.getProperty("debug-retrofit"))) {
            var interceptor = new HttpLoggingInterceptor();
            interceptor.level(HttpLoggingInterceptor.Level.BODY);
            okc.addInterceptor(interceptor);
        }
        okc.callTimeout(ReusableSteps.COMPONENT_SEARCH_TIMEOUT_DURATION);
        okc.connectTimeout(ReusableSteps.COMPONENT_SEARCH_TIMEOUT_DURATION);
        okc.readTimeout(ReusableSteps.COMPONENT_SEARCH_TIMEOUT_DURATION);
        okc.writeTimeout(ReusableSteps.COMPONENT_SEARCH_TIMEOUT_DURATION);
        client = okc.build();
        remoteRobot = new RemoteRobot(url, client);
    }

    @Override
    public void afterTestExecution(ExtensionContext context) throws Exception {
        if (context == null || context.getRequiredTestMethod() == null) {
            throw new IllegalStateException("test method is null");
        }
        var testMethod = context.getRequiredTestMethod();
        var testMethodName = testMethod.getName();
        var testFailed = context.getExecutionException().isPresent();
        if (testFailed) {
            saveScreenshot(testMethodName);
            saveIdeaFrames(testMethodName);
            saveHierarchy(testMethodName);
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext != null && parameterContext.getParameter() != null &&
               parameterContext.getParameter().getType() != null &&
               parameterContext.getParameter().getType().equals(RemoteRobot.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return remoteRobot;
    }

    private void saveScreenshot(String testName) throws IOException {
        var image = fetchScreenShot();
        save(image, testName);
    }

    private void saveHierarchy(String testName) throws IOException {
        var hierarchySnapshot = saveFile(url, "build/reports", "hierarchy-" + testName + ".html");
        if (!Files.exists(Paths.get("build/reports/styles.css"))) {
            saveFile(url + "/styles.css", "build/reports", "styles.css");
        }
        logger.warn("Hierarchy snapshot: {}", hierarchySnapshot.toAbsolutePath());
    }

    private Path saveFile(String fileurl, String folder, String name) throws IOException {
        Response response = client.newCall(new Request.Builder().url(fileurl).build()).execute();
        Files.createDirectories(Paths.get(folder));
        var content = response.body() == null || Strings.isNullOrEmpty(response.body().string()) ? "" : response.body().string();
        return Files.writeString(Paths.get(folder, name), content);
    }

    private void save(BufferedImage image, String name) throws IOException {
        var baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        var bytes = baos.toByteArray();
        Files.createDirectories(Paths.get("build/reports"));
        Files.write(Paths.get("build/reports/", name + ".png"), bytes);
    }

    private void saveIdeaFrames(String testName) throws IOException {
        var frames = remoteRobot.findAll(ContainerFixture.class, Locators.byXpath("//div[@class='IdeFrameImpl']"));
        for (int i = 0; i < frames.size(); i++) {
            var frame = frames.get(i);
            byte[] bytes = frame.callJs(
                """
                importPackage(java.io)
                importPackage(javax.imageio)
                importPackage(java.awt.image)
                const screenShot = new BufferedImage(component.getWidth(), component.getHeight(), BufferedImage.TYPE_INT_ARGB);
                component.paint(screenShot.getGraphics())
                let pictureBytes;
                const baos = new ByteArrayOutputStream();
                try {
                    ImageIO.write(screenShot, "png", baos);
                    pictureBytes = baos.toByteArray();
                } finally {
                    baos.close();
                }
                pictureBytes
                """, true);
            var image = ImageIO.read(new ByteArrayInputStream(bytes));
            save(image, testName + "_" + i);
        }
    }

    private BufferedImage fetchScreenShot() throws IOException {
        byte[] bytes = remoteRobot.callJs(
            """
            importPackage(java.io)
            importPackage(javax.imageio)
            const screenShot = new java.awt.Robot().createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));
            let pictureBytes;
            const baos = new ByteArrayOutputStream();
            try {
                ImageIO.write(screenShot, "png", baos);
                pictureBytes = baos.toByteArray();
            } finally {
              baos.close();
            }
            pictureBytes
            """);
        return ImageIO.read(new ByteArrayInputStream(bytes));
    }
}
