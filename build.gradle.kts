import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML

plugins {
    id("java") // Java support
    alias(libs.plugins.kotlin) // Kotlin support
    alias(libs.plugins.gradleIntelliJPlugin) // Gradle IntelliJ Plugin
    alias(libs.plugins.changelog) // Gradle Changelog Plugin
    alias(libs.plugins.qodana) // Gradle Qodana Plugin
    alias(libs.plugins.kover) // Gradle Kover Plugin
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

// Configure project's dependencies
repositories {
    mavenCentral()
}

// Set the JVM language level used to build the project. Use Java 11 for 2020.3+, and Java 17 for 2022.2+.
kotlin {
    jvmToolchain(17)
}

// Configure Gradle IntelliJ Plugin - read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    pluginName.set(providers.gradleProperty("pluginName"))
    version.set(providers.gradleProperty("platformVersion"))
    type.set(providers.gradleProperty("platformType"))

    // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file.
    plugins.set(
        providers.gradleProperty("platformPlugins").map { it.split(',').map(String::trim).filter(String::isNotEmpty) })
}

// Configure Gradle Changelog Plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    groups.empty()
    repositoryUrl.set(providers.gradleProperty("pluginRepositoryUrl"))
}

// Configure Gradle Qodana Plugin - read more: https://github.com/JetBrains/gradle-qodana-plugin
qodana {
    cachePath.set(provider { file(".qodana").canonicalPath })
    reportPath.set(provider { file("build/reports/inspections").canonicalPath })
    saveReport.set(true)
    showReport.set(providers.environmentVariable("QODANA_SHOW_REPORT").map { it.toBoolean() }.getOrElse(false))
}

// Configure Gradle Kover Plugin - read more: https://github.com/Kotlin/kotlinx-kover#configuration
koverReport {
    defaults {
        xml {
            onCheck = true
        }
    }
}

tasks {
    wrapper {
        gradleVersion = providers.gradleProperty("gradleVersion").get()
    }

    patchPluginXml {
        version.set(providers.gradleProperty("pluginVersion"))
        sinceBuild.set(providers.gradleProperty("pluginSinceBuild"))
        untilBuild.set(providers.gradleProperty("pluginUntilBuild"))

        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        pluginDescription.set(providers.fileContents(layout.projectDirectory.file("README.md")).asText.map {
            val start = "<!-- Plugin description -->"
            val end = "<!-- Plugin description end -->"

            with(it.lines()) {
                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end)).joinToString("\n").let(::markdownToHTML)
            }
        })

        val changelog = project.changelog // local variable for configuration cache compatibility
        // Get the latest available change notes from the changelog file
        changeNotes.set(providers.gradleProperty("pluginVersion").map { pluginVersion ->
            with(changelog) {
                renderItem(
                    (getOrNull(pluginVersion) ?: getUnreleased()).withHeader(false).withEmptySections(false),
                    Changelog.OutputType.HTML,
                )
            }
        })
    }

    // Configure UI tests plugin
    // Read more: https://github.com/JetBrains/intellij-ui-test-robot
    runIdeForUiTests {
        systemProperty("robot-server.port", "8082")
        systemProperty("ide.mac.message.dialogs.as.sheets", "false")
        systemProperty("jb.privacy.policy.text", "<!--999.999-->")
        systemProperty("jb.consents.confirmation.enabled", "false")
    }

    signPlugin {
        certificateChain.set(providers.environmentVariable("CERTIFICATE_CHAIN"))
        privateKey.set(providers.environmentVariable("PRIVATE_KEY"))
        password.set(providers.environmentVariable("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        dependsOn("patchChangelog")
        token.set(providers.environmentVariable("PUBLISH_TOKEN"))
        // The pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
        channels.set(providers.gradleProperty("pluginVersion").map {
            listOf(
                it.split('-').getOrElse(1) { "default" }.split('.').first()
            )
        })

    }

    runIde {

        val tempJvmArgs = ArrayList<String>()

        allJvmArgs.forEach {
            tempJvmArgs.add(it)
        }

        tempJvmArgs.add("--add-opens=java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED")
        tempJvmArgs.add("--add-opens=java.base/jdk.internal.org.objectweb.asm.tree=ALL-UNNAMED")
        tempJvmArgs.add("-javaagent:/Users/zhongchongtao/ja-netfilter.v3.1/ja-netfilter.jar")

        jvmArgs = tempJvmArgs
    }


    dependencies {
        implementation(fileTree(baseDir = "init"))

        annotationProcessor("org.projectlombok:lombok:1.18.2")
        compileOnly("org.projectlombok:lombok:1.18.2")
        testAnnotationProcessor("org.projectlombok:lombok:1.18.2")
        testCompileOnly("org.projectlombok:lombok:1.18.2")


        // https://mvnrepository.com/artifact/io.smallrye/smallrye-open-api-core
        implementation("io.smallrye:smallrye-open-api-core:3.10.0")

        // https://mvnrepository.com/artifact/com.konghq/unirest-java
        implementation("com.konghq:unirest-java:3.14.1")

        implementation("org.commonmark:commonmark:0.22.0")
        implementation("org.commonmark:commonmark-ext-gfm-tables:0.22.0")

        implementation("org.springframework:spring-core:6.1.10")
        // https://mvnrepository.com/artifact/org.apache.commons/commons-text
        implementation("org.apache.commons:commons-text:1.10.0")

        implementation("info.debatty:java-string-similarity:2.0.0")

        implementation("com.mashape.unirest:unirest-java:1.4.9")

        implementation("org.apache.poi:poi-ooxml:5.3.0")

        implementation("org.apache.poi:poi:5.3.0")

        implementation("commons-io:commons-io:2.16.0")

        implementation("org.apache.xmlbeans:xmlbeans:5.2.0")

//        implementation("org.apache.velocity:velocity:1.7")

        implementation("org.apache.commons:commons-compress:1.26.1")
        testImplementation("org.apache.logging.log4j:log4j-core:3.0.0-beta2")
    }
}

ext {
    dependencies {
        implementation("com.taobao.arthas:fastjson:1.2.80-fix")
    }
}