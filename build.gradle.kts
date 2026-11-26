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

    maven("https://r.irepo.space/maven/") {
        mavenContent {
            includeGroup("pers.neige.neigeitems")
        }
    }
    maven("https://repo.oraxen.com/releases") {
        mavenContent {
            includeGroup("io.th0rgal")
        }
    }
    maven("https://repo.momirealms.net/releases/") {
        mavenContent {
            includeGroup("net.momirealms")
        }
    }
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

    // https://github.com/ankhorg/NeigeItems-Kotlin
    compileOnly("pers.neige.neigeitems:NeigeItems:1.22.2") { isTransitive = false }
    // https://docs.oraxen.com/developers/api
    compileOnly("io.th0rgal:oraxen:1.221.0") { isTransitive = false }
    // https://github.com/Xiao-MoMi/craft-engine
    compileOnly("net.momirealms:craft-engine-core:26.9") { isTransitive = false }

    testImplementation(kotlin("test"))
    testImplementation(libs.paper.api)
    testImplementation("org.xerial:sqlite-jdbc:3.50.3.0")
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
