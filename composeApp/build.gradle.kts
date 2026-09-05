import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

kotlin {
    jvm("desktop")

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
            implementation("io.ktor:ktor-client-core:3.5.1")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
        }
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(compose.material3)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "app.panelrelay.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageName = "Panel Relay"
            packageVersion = "0.1.0"
            description = "A local-first manga reader"
            vendor = "Panel Relay"
        }
    }
}

tasks.register<JavaExec>("smokeSource") {
    group = "verification"
    description = "Loads the configured manga.in.ua title without downloading images."
    dependsOn("desktopMainClasses")
    val desktop = kotlin.targets.getByName("desktop").compilations.getByName("main")
    classpath(desktop.output.allOutputs, configurations.getByName("desktopRuntimeClasspath"))
    mainClass.set("app.panelrelay.SourceSmokeKt")
}

