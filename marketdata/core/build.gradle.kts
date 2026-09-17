plugins {
    id("buildlogic.java-library-conventions")
    `java-test-fixtures`
}

base {
    archivesName.set("marketdata-core")
}

dependencies {
    testFixturesApi("org.junit.jupiter:junit-jupiter-api:6.0.1")
}
