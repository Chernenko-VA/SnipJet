import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform") version "2.4.0"
    id("org.jetbrains.compose") version "1.11.1"
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.0"
}

group = "ru.chernenko"
version = "1.0.0"

repositories {
    mavenCentral()
    google()
}

kotlin {
    jvm()

    sourceSets {
        jvmMain.dependencies {
            val compose = "1.11.1"
            val material3 = "1.9.0"
            // Linux target (Ubuntu / Wayland). Replace artifact if building for another OS/arch.
            implementation("org.jetbrains.compose.desktop:desktop-jvm-linux-x64:$compose")
            implementation("org.jetbrains.compose.foundation:foundation:$compose")
            implementation("org.jetbrains.compose.material3:material3:$material3")
            implementation("org.jetbrains.compose.runtime:runtime:$compose")
            implementation("org.jetbrains.compose.ui:ui:$compose")
            // Loads application.yml (no Spring on desktop)
            implementation("org.yaml:snakeyaml:2.3")
        }
        jvmTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

/** Runtime packages SnipJet needs on Ubuntu/Debian (capture + Wayland clipboard). */
val snipJetDebDepends = listOf("gnome-screenshot", "wl-clipboard")

compose.desktop {
    application {
        mainClass = "ru.chernenko.snipjet.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Deb)
            packageName = "SnipJet"
            packageVersion = "1.0.0"
            description = "Screenshot capture and annotation for Linux Wayland"
            copyright = "© 2026"
            vendor = "SnipJet"
            linux {
                // Debian package name must be lowercase.
                packageName = "snipjet"
                debMaintainer = "snipjet@localhost"
                menuGroup = "Utility"
                appCategory = "Utility"
                shortcut = true
                iconFile.set(project.file("src/jvmMain/resources/icon.png"))
            }
        }
    }
}

/**
 * Compose Desktop 1.11.1 has no linux.packageDeps / StartupWMClass DSL.
 * After jpackage builds the .deb: merge Depends and fix .desktop for GNOME dock icons.
 */
val snipJetStartupWmClass = "ru-chernenko-snipjet-MainKt"

tasks.register("injectDebDepends") {
    group = "compose desktop"
    description = "Add runtime Depends and StartupWMClass to the packaged .deb"
    doLast {
        val debDir = layout.buildDirectory.dir("compose/binaries/main/deb").get().asFile
        val debFile = debDir.listFiles()?.singleOrNull { it.extension == "deb" }
            ?: error("No .deb found in ${debDir.absolutePath}. Run packageDeb first.")

        val workDir = layout.buildDirectory.dir("tmp/deb-depends").get().asFile
        workDir.deleteRecursively()
        workDir.mkdirs()

        fun runDpkg(vararg args: String) {
            val result = ProcessBuilder(*args)
                .inheritIO()
                .start()
                .waitFor()
            require(result == 0) { "Command failed ($result): ${args.joinToString(" ")}" }
        }

        val rootDir = workDir.resolve("root")
        runDpkg("dpkg-deb", "-R", debFile.absolutePath, rootDir.absolutePath)

        val controlFile = rootDir.resolve("DEBIAN/control")
        require(controlFile.isFile) { "Missing DEBIAN/control in ${debFile.name}" }

        val controlLines = controlFile.readLines().toMutableList()
        val dependsIndex = controlLines.indexOfFirst { it.startsWith("Depends:") }
        val extra = snipJetDebDepends.joinToString(", ")
        if (dependsIndex >= 0) {
            val existing = controlLines[dependsIndex].removePrefix("Depends:").trim()
            val merged = (existing.split(',').map { it.trim() }.filter { it.isNotEmpty() } + snipJetDebDepends)
                .distinct()
                .joinToString(", ")
            controlLines[dependsIndex] = "Depends: $merged"
        } else {
            controlLines.add("Depends: $extra")
        }
        controlFile.writeText(controlLines.joinToString("\n") + "\n")

        fun patchDesktopFile(desktop: java.io.File) {
            val lines = desktop.readLines().toMutableList()
            fun setOrAdd(key: String, value: String) {
                val index = lines.indexOfFirst { it.startsWith("$key=") }
                val entry = "$key=$value"
                if (index >= 0) lines[index] = entry else lines.add(entry)
            }
            setOrAdd("StartupWMClass", snipJetStartupWmClass)
            setOrAdd("StartupNotify", "true")
            desktop.writeText(lines.joinToString("\n") + "\n")
        }

        rootDir.walkTopDown()
            .filter { it.isFile && it.extension == "desktop" }
            .forEach { patchDesktopFile(it) }

        val rebuilt = debDir.resolve(debFile.name)
        runDpkg("dpkg-deb", "-b", rootDir.absolutePath, rebuilt.absolutePath)
        logger.lifecycle("Updated Depends and StartupWMClass in ${rebuilt.absolutePath}")
    }
}

afterEvaluate {
    tasks.named("packageDeb").configure {
        finalizedBy("injectDebDepends")
    }
}
