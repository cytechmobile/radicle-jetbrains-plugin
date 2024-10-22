import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML

fun properties(key: String) = providers.gradleProperty(key)
fun environment(key: String) = providers.environmentVariable(key)

plugins {
    // Java support
    id("java")
    checkstyle
    alias(libs.plugins.kotlin) // Kotlin support
    alias(libs.plugins.gradleIntelliJPlugin) // Gradle IntelliJ Plugin
    alias(libs.plugins.changelog) // Gradle Changelog Plugin
}

group = properties("pluginGroup").get()
version = properties("pluginVersion").get()

// Configure project's dependencies
repositories {
    mavenCentral()
    maven {
        url = uri("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
    }
}

var remoteRobotVersion: String = libs.versions.remoteRobot.get();
dependencies {
    testImplementation("com.intellij.remoterobot:remote-robot:$remoteRobotVersion")
    testImplementation("com.intellij.remoterobot:remote-fixtures:$remoteRobotVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${libs.versions.junitJupiter.get()}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${libs.versions.junitJupiter.get()}")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:${libs.versions.junitJupiter.get()}")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:${libs.versions.junitPlatformLauncher.get()}")

    testImplementation("org.assertj:assertj-core:${libs.versions.assertj.get()}")

    // Logging Network Calls
    testImplementation("com.squareup.okhttp3:logging-interceptor:${libs.versions.loggingInterceptor.get()}")
    testImplementation("org.mockito:mockito-core:${libs.versions.mockito.get()}")

    // Deserialize timestamps to Instant
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:${libs.versions.jacksonJsr310.get()}")
    implementation("org.apache.tika:tika-core:${libs.versions.tika.get()}")
    implementation("com.automation-remarks:video-recorder-junit5:2.+")
    implementation("com.sshtools:maverick-synergy-client:${libs.versions.sshTools.get()}")
}

checkstyle {
//    configFile = File("./checkstyle.xml");
    // we do not tolerate errors or warnings, break build if any found
    toolVersion = libs.versions.checkstyleTools.get();
    maxErrors = 0;
    maxWarnings = 0;
}

// Set the JVM language level used to build the project.
kotlin {
    jvmToolchain(17)
}

// Configure Gradle IntelliJ Plugin - read more: https://github.com/JetBrains/gradle-intellij-plugin
intellij {
    pluginName = properties("pluginName")
    version = properties("platformVersion")
    type = properties("platformType")

    // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file.
    plugins = properties("platformPlugins").map { it.split(',').map(String::trim).filter(String::isNotEmpty) }
}

// Configure Gradle Changelog Plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
//    version.set(properties("pluginVersion"))
    groups.empty()
    repositoryUrl = properties("pluginRepositoryUrl")
}

tasks {
    // Set the JVM compatibility versions
    properties("javaVersion").get().let {
        withType<JavaCompile> {
            sourceCompatibility = it
            targetCompatibility = it
        }
    }

    wrapper {
        gradleVersion = properties("gradleVersion").get()
    }

    patchPluginXml {
        version = properties("pluginVersion")
        sinceBuild = properties("pluginSinceBuild")
        untilBuild = properties("pluginUntilBuild")

        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        pluginDescription = providers.fileContents(layout.projectDirectory.file("README.md")).asText.map {
            val start = "<!-- Plugin description -->"
            val end = "<!-- Plugin description end -->"
            with(it.lines()) {
                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end)).joinToString("\n").let(::markdownToHTML)
            }
        }

        val changelog = project.changelog // local variable for configuration cache compatibility
        // Get the latest available change notes from the changelog file
        changeNotes = properties("pluginVersion").map { pluginVersion ->
            with(changelog) {
                renderItem(
                    (getOrNull(pluginVersion) ?: getUnreleased())
                        .withHeader(false)
                        .withEmptySections(false),
                    Changelog.OutputType.HTML,
                )
            }
        }
    }

    downloadRobotServerPlugin {
        version = remoteRobotVersion
    }

    runIde {
        systemProperty("idea.is.internal", "true");
    }

    // Configure UI tests plugin
    // Read more: https://github.com/JetBrains/intellij-ui-test-robot
    runIdeForUiTests {
        //    In case your Idea is launched on remote machine you can enable public port and enable encryption of JS calls
        //    systemProperty "robot-server.host.public", "true"
        //    systemProperty "robot.encryption.enabled", "true"
        //    systemProperty "robot.encryption.password", "my super secret"

        systemProperty("robot-server.port", "8082")
        systemProperty("ide.mac.message.dialogs.as.sheets", "false")
        systemProperty("jb.privacy.policy.text", "<!--999.999-->")
        systemProperty("jb.consents.confirmation.enabled", "false")
        systemProperty("ide.mac.file.chooser.native", "false")
        systemProperty("apple.laf.useScreenMenuBar", "false")
        systemProperty("jbScreenMenuBar.enabled", "false")
        systemProperty("idea.trust.all.projects", "true")
        systemProperty("ide.show.tips.on.startup.default.value", "false")
    }

    test {
        // enable here nad in runIdeForUiTests block - to log the retrofit HTTP calls
        // systemProperty "debug-retrofit", "enable"

        // enable encryption on test side when use remote machine
        // systemProperty "robot.encryption.password", "my super secret"
        exclude("network/radicle/jetbrains/radiclejetbrainsplugin/remoterobot/**")
    }

    signPlugin {
        certificateChain = environment("CERTIFICATE_CHAIN")
        privateKey = environment("PRIVATE_KEY")
        password = environment("PRIVATE_KEY_PASSWORD")
    }

    publishPlugin {
        dependsOn("patchChangelog")
        token = environment("PUBLISH_TOKEN")
        // pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
        // channels = properties("pluginVersion").map { listOf(it.split('-').getOrElse(1) { "default" }.split('.').first()) }
        channels = properties("pluginVersion").map { listOf(it.substringAfter('-', "").substringBefore('.').ifEmpty { "default" }) }
    }
}

val uiTestTask = tasks.register<Test>("uiTest") {
    useJUnitPlatform()
    testLogging { exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL }
    environment(mapOf("RADICLE_REPO" to System.getenv("RADICLE_REPO")))
    include("network/radicle/jetbrains/radiclejetbrainsplugin/remoterobot/ui/**")
}

val e2eTestTask = tasks.register<Test>("endToEndTests") {
    useJUnitPlatform()
    testLogging { exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL }
    environment(mapOf("RADICLE_REPO" to System.getenv("RADICLE_REPO")))
    include("network/radicle/jetbrains/radiclejetbrainsplugin/remoterobot/e2e/**")
}