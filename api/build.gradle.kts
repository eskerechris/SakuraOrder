plugins {
    `java-library`
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly(libs.paper.api)
    compileOnly(libs.bundles.database)
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}