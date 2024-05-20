import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.date
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.models.ProductRelease
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java")
    checkstyle
    alias(libs.plugins.kotlin) // Kotlin support
    id("org.jetbrains.intellij.platform") version "2.0.0-beta2"
    id("org.jetbrains.intellij.platform.migration") version "2.0.0-beta2"
    alias(libs.plugins.changelog) // Gradle Changelog Plugin
}

group = providers.gradleProperty("pluginGroup")
version = providers.gradleProperty("pluginVersion")

repositories {
    mavenCentral()
    maven {
        url = uri("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
    }
    intellijPlatform {
        defaultRepositories()
        jetbrainsRuntime()
        intellijDependencies()
    }
}

dependencies {
    intellijPlatform {
        create(
            providers.gradleProperty("platformType"),
            providers.gradleProperty("platformVersion")
        )
        bundledPlugins(providers.gradleProperty("platformPlugins").map { it.split(',') }.getOrElse(emptyList()))
         instrumentationTools()
        // pluginVerifier()
        // zipSigner()
    }

    // testImplementation("com.intellij.remoterobot:remote-robot:$remoteRobotVersion")
    // testImplementation("com.intellij.remoterobot:remote-fixtures:$remoteRobotVersion")
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

// Read more: https://github.com/JetBrains/gradle-intellij-plugin
intellijPlatform {
    pluginConfiguration {
        id = providers.gradleProperty("pluginId")
        name = providers.gradleProperty("pluginName")
        version = providers.gradleProperty("pluginVersion")

        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        description = providers.fileContents(layout.projectDirectory.file("./README.md")).asText.map {
            it.lines().run {
                val start = "<!-- Plugin description -->"
                val end = "<!-- Plugin description end -->"

                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end))
            }.joinToString("\n")
        }.map {
            markdownToHTML(it)
        }

        // Get the latest available change notes from the changelog file
        changeNotes.set(provider {
            changelog.renderItem(
                changelog.getLatest(),
                Changelog.OutputType.HTML
            )
        })

        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            untilBuild = provider { null } // removes until-build in plugin.xml
        }

        vendor {
            name = providers.gradleProperty("pluginVendor")
            url = providers.gradleProperty("pluginVendorUrl")
        }
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")

        // pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
        channels = providers.gradleProperty("pluginVersion").map {
            Regex(".+-(\\[a-zA-Z]+).*")
                .find(it)
                ?.groupValues
                ?.getOrNull(1)
                ?: "default"
        }.map { listOf(it) }
    }

    // verifyPlugin {
    //     ides {
    // //         ides(providers.gradleProperty("pluginVerifierIdeVersions").map { it.split(',') }.getOrElse(emptyList()))
    // //         recommended()
    // //         // channels = listOf(ProductRelease.Channel.RELEASE)
    //
    // // try
    // //         select {
    // //             types = listOf(IntelliJPlatformType.IntellijIdeaCommunity)
    // //             channels = listOf(ProductRelease.Channel.RELEASE)
    // //             sinceBuild = "223"
    // //             untilBuild = "241.*"
    // //         }
    //    }
    // }
}

// Read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    version = providers.gradleProperty("pluginVersion")
    path = "${rootProject.projectDir}/CHANGELOG.md"
    header = provider { "[${version.get()}] - ${date()}" }
    itemPrefix = "-"
    keepUnreleasedSection = true
    unreleasedTerm = "[Unreleased]"
    groups = emptyList()
    // groups.set(listOf("Added", "Changed", "Deprecated", "Removed", "Fixed", "Security"))
}

// Java 11 compat started in 2020.3
// Java 17 compat started in 2021.3
// Java 21 compat started in 2024.2
val jvmLanguageLevel = 17

kotlin {
    jvmToolchain(jvmLanguageLevel)
}

tasks {
    withType<JavaCompile> {
        options.release.set(jvmLanguageLevel)
    }

    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "$jvmLanguageLevel"

            // Match the lowest supported version for this platform
            // See https://plugins.jetbrains.com/docs/intellij/kotlin.html#kotlin-standard-library
            apiVersion = "1.8"
            languageVersion = "1.8"

            // Generates default method in Kotlin interfaces to be usable from Java
            // https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-default/
            freeCompilerArgs += "-Xjvm-default=all"
        }
    }

    withType<Test> {
        useJUnitPlatform()
        // needed for com.intellij.testFramework.UsefulTestCase.DELETE_ON_EXIT_HOOK_CLASS
        jvmArgs("--add-opens", "java.base/java.io=ALL-UNNAMED")
    }


    printProductsReleases {
        channels = listOf(ProductRelease.Channel.EAP)
        types = listOf(IntelliJPlatformType.IntellijIdeaCommunity)
        untilBuild = provider { null }

        doLast {
            val latestEap = productsReleases.get().max()
            println("latest eap " + latestEap)
        }
    }

    // runPluginVerifier {
    //     ideVersions = properties("pluginVerifierIdeVersions").split(',').map(String::trim).filter(String::isNotEmpty)
    // }

    runIde {
        // dependsOn(processResources)
        systemProperty("idea.is.internal", "true");
    }

    // val runIdeUltimate by registering(CustomRunIdeTask::class) {
    //     type = IntelliJPlatformType.IntellijIdeaUltimate
    //     version = providers.gradleProperty("intellij.version")
    //
    //     applySystemProperties()
    // }

    // Latest available EAP release
    // https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-faq.html#how-to-check-the-latest-available-eap-release


    publishPlugin {
        dependsOn(patchChangelog)
    }
}

