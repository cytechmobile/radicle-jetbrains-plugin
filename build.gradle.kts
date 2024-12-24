import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML

fun properties(key: String) = providers.gradleProperty(key)
fun environment(key: String) = providers.environmentVariable(key)

plugins {
    // Java support
    id("java")
    checkstyle
    alias(libs.plugins.gradleIntelliJPlugin) // IntelliJ Platform Gradle Plugin
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
    intellijPlatform {
        defaultRepositories()
    }
}

var remoteRobotVersion: String = libs.versions.remoteRobot.get();
dependencies {
    intellijPlatform {
        //intellijIdeaCommunity('LATEST-EAP-SNAPSHOT')
        intellijIdeaCommunity(properties("platformVersion").get())
        // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file.
        bundledPlugins(providers.gradleProperty("platformPlugins").map { it.split(',').map { it2 -> it2.trim()} })
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
    }
    //https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-faq.html#missing-opentest4j-dependency-in-test-framework
    //testImplementation("junit:junit:4.13.2")
    //testImplementation("org.opentest4j:opentest4j:1.3.0")

    testImplementation("org.junit.jupiter:junit-jupiter-api:${libs.versions.junitJupiter.get()}")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:${libs.versions.junitJupiter.get()}")
    testImplementation("org.junit.vintage:junit-vintage-engine:${libs.versions.junitJupiter.get()}")
    testImplementation("org.junit.platform:junit-platform-launcher:${libs.versions.junitPlatformLauncher.get()}")

    testImplementation("org.assertj:assertj-core:${libs.versions.assertj.get()}")

    testImplementation("com.intellij.remoterobot:remote-robot:$remoteRobotVersion")
    testImplementation("com.intellij.remoterobot:remote-fixtures:$remoteRobotVersion")

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

// Configure IntelliJ Platform Gradle Plugin - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
intellijPlatform {
    pluginConfiguration {
        name = properties("pluginName")
    }

    pluginConfiguration {
        ideaVersion {
            sinceBuild = properties("pluginSinceBuild")
            untilBuild = provider { null } // properties("pluginUntilBuild")
        }
    }

    pluginVerification {
        ides {
            recommended()
        }
    }
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
        version = properties("pluginVersion").get()
        sinceBuild = properties("pluginSinceBuild").get()
        untilBuild = provider { null } // properties("pluginUntilBuild")

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

    runIde {
        systemProperty("idea.is.internal", "true");
    }

    test {
        // enable here nad in runIdeForUiTests block - to log the retrofit HTTP calls
        // systemProperty "debug-retrofit", "enable"

        // enable encryption on test side when use remote machine
        // systemProperty "robot.encryption.password", "my super secret"
        exclude("network/radicle/jetbrains/radiclejetbrainsplugin/remoterobot/**")
        testLogging {
            // set options for log level LIFECYCLE
            events = arrayOf(TestLogEvent.FAILED, /*TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.STANDARD_OUT*/).toSet()
            exceptionFormat = TestExceptionFormat.FULL
            showExceptions = true
            showCauses = true
            showStackTraces = true
        }
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

val runIdeForUiTests by intellijPlatformTesting.runIde.registering {
    task {
        jvmArgumentProviders += CommandLineArgumentProvider {
            listOf(
                "-Drobot-server.port=8082",
                "-Dide.mac.message.dialogs.as.sheets=false",
                "-Djb.privacy.policy.text=<!--999.999-->",
                "-Djb.consents.confirmation.enabled=false",
                "-Dide.mac.file.chooser.native=false",
                "-Dapple.laf.useScreenMenuBar=false",
                "-DjbScreenMenuBar.enabled=false",
                "-Didea.trust.all.projects=true",
                "-Dide.show.tips.on.startup.default.value=false",
            )
        }
    }

    plugins {
        robotServerPlugin()
    }
}


val e2eTestTask = tasks.register<Test>("endToEndTests") {
    useJUnitPlatform()
    testLogging { exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL }
    environment(mapOf("RADICLE_REPO" to System.getenv("RADICLE_REPO")))
    include("network/radicle/jetbrains/radiclejetbrainsplugin/remoterobot/e2e/**")
}