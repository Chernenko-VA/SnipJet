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

compose.desktop {
    application {
        mainClass = "ru.chernenko.snipjet.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "SnipJet"
            packageVersion = "1.0.0"
            description = "Screenshot capture and annotation for Linux Wayland"
            copyright = "© 2026"
            linux {
                iconFile.set(project.file("src/jvmMain/resources/icon.png"))
            }
        }
    }
}
