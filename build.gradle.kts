plugins {
    java
    alias(libs.plugins.run.paper)
    id("com.gradleup.shadow") version "9.6.1"
}

repositories {
    mavenCentral()

    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        name = "vault"
        url = uri("https://jitpack.io")
    }
    maven {
        name = "folialib"
        url = uri("https://repo.tcoded.com/releases")
    }
}

dependencies {
    implementation(project(":api"))
    compileOnly(libs.paper.api)
    compileOnly(libs.bundles.database)
    compileOnly(libs.vault) {
        exclude(group = "org.bukkit", module = "bukkit")
    }
    implementation(libs.bstats)
    implementation(libs.folialib)
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks {
    runServer {
        minecraftVersion(libs.versions.minecraft.get())
        runDirectory(layout.projectDirectory.dir("run").asFile)
        jvmArgs("-Xms2G", "-Xmx2G")
    }

    shadowJar {
        archiveBaseName.set("SakuraOrder")
        archiveClassifier.set("")
        relocate("org.bstats", "me.chris.sakuraOrder.libs.bstats")
        relocate("com.tcoded.folialib", "me.chris.sakuraOrder.libs.folialib")
    }

    build {
        dependsOn(shadowJar)
    }

    processResources {
        val props = mapOf(
            "version" to version,
            "hikariVersion" to libs.versions.hikari.get(),
            "sqliteVersion" to libs.versions.sqlite.get(),
            "mysqlVersion" to libs.versions.mysql.get(),
            "mariadbVersion" to libs.versions.mariadb.get()
        )
        inputs.properties(props)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}