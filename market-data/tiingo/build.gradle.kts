plugins {
    id("buildlogic.java-library-conventions")
}

base {
    archivesName.set("marketdata-tiingo")
}

dependencies {
    api(project(":market-data:core"))
    implementation("com.fasterxml.jackson.core:jackson-databind:2.20.1")
    testImplementation(testFixtures(project(":market-data:core")))
}
