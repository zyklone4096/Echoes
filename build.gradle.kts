import org.jetbrains.kotlin.gradle.utils.extendsFrom
import xyz.jpenilla.resourcefactory.bukkit.BukkitPluginYaml

plugins {
    kotlin("jvm") version libs.versions.kotlin.get()
    alias(libs.plugins.shadow)
    alias(libs.plugins.run.paper)
    alias(libs.plugins.resource.factory.paper)
    alias(libs.plugins.kotlinx.serialization)
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

val include = configurations.create("include")
configurations.implementation.get().extendsFrom(include)

dependencies {
    compileOnly(libs.paper.api)
    include(libs.kotlin.stdlib)

    include(libs.kotlinx.serialization.hocon)

    include(libs.exposed.core)
    include(libs.exposed.jdbc)
    include(libs.exposed.migration)
    include(libs.exposed.dao)
    include(libs.hikari)

    include(libs.cloud.paper)
    include(libs.cloud.kotlin.coroutines)
}

paperPluginYaml {
    main = "dev.zyklone.echoes.Echoes"
    apiVersion = "26.3"

    load = BukkitPluginYaml.PluginLoadOrder.STARTUP
    authors.addAll("Zyklone")
}

kotlin {
    jvmToolchain(25)
}

tasks {
    build {
        dependsOn(shadowJar)
    }

    shadowJar {
        configurations = setOf(include)
    }

    runServer {
        minecraftVersion(libs.versions.minecraft.get())
        jvmArgs("-Xms2G", "-Xmx2G", "-Dcom.mojang.eula.agree=true", "-Ddisable.watchdog=true", "-XX:+AllowEnhancedClassRedefinition")
    }
}
