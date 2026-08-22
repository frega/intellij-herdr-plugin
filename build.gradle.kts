import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij.platform")
    id("org.jetbrains.changelog")
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.add("-Xjvm-default=all")
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")

    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        // pty4j and JediTerm classes come from the PhpStorm distribution itself (lib/intellij.libraries.pty4j.jar,
        // lib/intellij.libraries.jediterm.*.jar), so no separate dependency is declared for them.
        phpstorm("2026.2")
        testFramework(TestFrameworkType.Platform)
    }
}
