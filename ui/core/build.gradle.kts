plugins {
    id("buildlogic.java-library-conventions")
    id("org.openjfx.javafxplugin") version "0.1.0"
}

base {
    archivesName.set("ui-core")
}

dependencies {
    api(project(":ui:icons"))
}

javafx {
    version = "25.0.1"
    modules = listOf("javafx.controls")
}

tasks.withType<Test>().configureEach {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    // Keep focused JavaFX stages from competing with the icon tests.
    mustRunAfter(":ui:icons:test")
}
