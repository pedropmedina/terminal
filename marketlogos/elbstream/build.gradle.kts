plugins {
    id("buildlogic.java-library-conventions")
}

base {
    archivesName.set("marketlogos-elbstream")
}

dependencies {
    api(project(":marketlogos:core"))
}
