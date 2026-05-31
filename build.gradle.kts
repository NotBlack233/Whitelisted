plugins {
    kotlin("jvm") version "2.4.0-RC"
    kotlin("plugin.serialization") version "2.4.0-RC"
    id("com.gradleup.shadow") version "9.4.1"
    id("xyz.jpenilla.run-velocity") version "3.0.2"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("com.velocitypowered:velocity-api:3.5.0-SNAPSHOT")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.xerial:sqlite-jdbc:3.53.1.0")

    implementation(platform("org.http4k:http4k-bom:6.47.2.0"))
    implementation("org.http4k:http4k-core")
    implementation("org.http4k:http4k-server-undertow")
    implementation("org.http4k:http4k-client-apache")

    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
}

kotlin {
    jvmToolchain(21)
}

tasks {
    build {
        dependsOn(shadowJar)
    }

    runVelocity {
        // Configure the Velocity version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        velocityVersion("3.5.0-SNAPSHOT")
    }

    processResources {
        val props = mapOf("version" to version)
        filesMatching("velocity-plugin.json") {
            expand(props)
        }
    }
}
