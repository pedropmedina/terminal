import java.nio.file.Files
import java.nio.file.StandardCopyOption
import org.gradle.api.plugins.JavaApplication

plugins {
    java
    id("org.openjfx.javafxplugin") version "0.1.0"
}

repositories {
    mavenCentral()
}

// The application is configured first so this project can use its sources and external dependencies.
evaluationDependsOn(":app")
val appProject = project(":app")
val appSourceSets = appProject.extensions.getByType<SourceSetContainer>()

val hotswapAgent = configurations.create("hotswapAgent") {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
}

// These sources are never part of the main JAR. Gradle's regular source-set task
// compiles against the app; compileHotJava produces the Java 25 runtime classes.
val hotReload = sourceSets.create("hotreload") {
    compileClasspath += configurations.compileClasspath.get()
    runtimeClasspath += output + compileClasspath
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(26)
    }
}

dependencies {
    implementation(project(":app"))
    add(hotReload.compileOnlyConfigurationName, "org.hotswapagent:hotswap-agent-core:2.0.3")
    add(hotswapAgent.name, "org.hotswapagent:hotswap-agent:2.0.3")
}

// JetBrains Runtime supports enhanced class redefinition on Java 25. Normal builds
// target Java 26, so compile application and library sources again for the dev runtime.
val hotCompilerWorkspace = "emulator-hot-${rootProject.projectDir.absolutePath.hashCode()}"
val compiledHotClassesDirectory = layout.dir(providers.provider {
    file("${System.getProperty("java.io.tmpdir")}/$hotCompilerWorkspace/classes")
})
val hotClassesDirectory = layout.buildDirectory.dir("classes/java/hot")
val hotResourcesDirectory = layout.buildDirectory.dir("resources/hot")

val libraryPaths = listOf("market-data/core", "market-data/tiingo", "market-logos/core", "market-logos/elbstream", "ui/core", "ui/icons")
// Resolve through this project's dependency graph; project JARs target Java 26 and
// are replaced by the Java 25 classes compiled from source below.
val externalCompileClasspath = configurations.compileClasspath.get().incoming.artifactView {
    componentFilter { it !is org.gradle.api.artifacts.component.ProjectComponentIdentifier }
}.files
val externalRuntimeClasspath = configurations.runtimeClasspath.get().incoming.artifactView {
    componentFilter { it !is org.gradle.api.artifacts.component.ProjectComponentIdentifier }
}.files

val compileHotJava = tasks.register<JavaCompile>("compileHotJava") {
    group = "application"
    description = "Compiles Java 25-compatible classes for HotswapAgent."

    source(appSourceSets.named("main").get().java)
    source(hotReload.java)
    libraryPaths.forEach { source(rootProject.file("$it/src/main/java")) }
    classpath = externalCompileClasspath + externalRuntimeClasspath + configurations.getByName(hotReload.compileClasspathConfigurationName)
    destinationDirectory.set(compiledHotClassesDirectory)
    javaCompiler.set(javaToolchains.compilerFor {
        languageVersion.set(JavaLanguageVersion.of(25))
        vendor.set(JvmVendorSpec.JETBRAINS)
    })
    options.release.set(25)
    options.encoding = "UTF-8"

    // Continuous mode invokes this task only after an input change. Recompiling avoids
    // reusing a stale snapshot after a rapid save.
    outputs.upToDateWhen { false }
}

val publishHotClasses = tasks.register("publishHotClasses") {
    group = "application"
    description = "Copies compiled classes into the stable directory watched by HotswapAgent."
    dependsOn(compileHotJava)
    doNotTrackState("The staging directory must not become a continuous-build watch input.")

    doLast {
        val sourceRoot = compiledHotClassesDirectory.get().asFile.toPath()
        val targetRoot = hotClassesDirectory.get().asFile.toPath()

        val paths = Files.walk(sourceRoot)
        try {
            paths.filter { Files.isRegularFile(it) }.forEach { source ->
                val target = targetRoot.resolve(sourceRoot.relativize(source))
                Files.createDirectories(target.parent)

                if (!Files.exists(target) || Files.mismatch(source, target) != -1L) {
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING)
                }
            }
        } finally {
            paths.close()
        }
    }
}

val processHotResources = tasks.register<Copy>("processHotResources") {
    group = "application"
    description = "Copies application resources for the hot-reload runtime."

    from(appSourceSets.named("main").get().resources)
    from(hotReload.resources)
    libraryPaths.forEach { from(rootProject.file("$it/src/main/resources")) }
    into(hotResourcesDirectory)
}

val hotClasses = tasks.register("hotClasses") {
    group = "application"
    description = "Builds the classes and resources watched by HotswapAgent."
    dependsOn(publishHotClasses, processHotResources)
}

tasks.register<JavaExec>("hotRun") {
    group = "application"
    description = "Runs the JavaFX application with enhanced class redefinition."
    dependsOn(hotClasses)
    workingDir(rootProject.projectDir)

    mainClass.set(appProject.extensions.getByType<JavaApplication>().mainClass)
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(25))
        vendor.set(JvmVendorSpec.JETBRAINS)
    })
    jvmArgs(
        "-XX:+AllowEnhancedClassRedefinition",
        "-XX:HotswapAgent=external",
        "-Xlog:redefine+class*=info",
    )

    doFirst {
        val runtimeFiles = externalRuntimeClasspath.files
        val javaFxFiles = runtimeFiles.filter { it.name.startsWith("javafx-") }
        val applicationFiles = runtimeFiles - javaFxFiles.toSet()

        classpath = files(hotClassesDirectory, hotResourcesDirectory, applicationFiles)
        jvmArgs(
            "--module-path", javaFxFiles.joinToString(File.pathSeparator),
            "--add-modules", "javafx.controls",
            "--enable-native-access=javafx.graphics",
            "-javaagent:${hotswapAgent.singleFile.absolutePath}=autoHotswap=true",
        )
    }
}

javafx {
    version = "25.0.1"
    modules = listOf("javafx.controls")
}
