plugins {
    kotlin("jvm") version "2.0.20"
    id("com.gradleup.shadow") version "8.3.1"
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

group = "cat.aubrey"
version = "1.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://repo.aikar.co/content/groups/aikar/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("co.aikar:acf-paper:0.5.1-SNAPSHOT")
    implementation("org.xerial:sqlite-jdbc:3.46.1.0")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("org.mongodb:mongodb-driver-sync:5.1.4")

}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        minimize()
        relocate("co.aikar.commands", "cat.aubrey.ivy.acf")
        relocate("co.aikar.locales", "cat.aubrey.ivy.locales")
    }

    build {
        dependsOn(shadowJar)
    }

    processResources {
        filesMatching("plugin.yml") {
            expand("version" to project.version)
        }
    }

    runServer {
        minecraftVersion("1.21.1")
    }
}