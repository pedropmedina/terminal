plugins {
    id("buildlogic.java-library-conventions")
    `java-test-fixtures`
}

base {
    archivesName.set("marketdata-core")
}

sourceSets.named("testFixtures") {
    java.setSrcDirs(listOf("src/test-fixtures/java"))
    resources.setSrcDirs(listOf("src/test-fixtures/resources"))
}

dependencies {
    testFixturesApi("org.junit.jupiter:junit-jupiter-api:6.0.1")
}
