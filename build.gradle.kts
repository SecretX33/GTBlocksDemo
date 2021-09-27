import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.31"
    kotlin("kapt") version "1.5.31"
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

group = "com.github.secretx33"
version = "1.0"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://repo.codemc.org/repository/maven-public/")
    maven("https://papermc.io/repo/repository/maven-public/")
    maven("https://plugins.gradle.org/m2/")
    maven("https://jitpack.io")
    maven("https://maven.enginehub.org/repo/")
    maven("https://repo.mattstudios.me/artifactory/public")
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:1.5.31"))
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")
    val exposedVersion = "0.35.1"
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("com.zaxxer:HikariCP:4.0.3")  // version 5+ does not support Java 8
    // Toothpick is an amazing DI library, but it doesn't support newer java versions (at least on this version), so keep that in mind when trying to updating java version
    val toothpick_version = "3.1.0"
    implementation("com.github.stephanenicolas.toothpick:ktp:$toothpick_version")
    kapt("com.github.stephanenicolas.toothpick:toothpick-compiler:$toothpick_version")
    // Paper API
    compileOnly("com.destroystokyo.paper:paper-api:1.12.2-R0.1-SNAPSHOT")
    implementation("com.github.cryptomorin:XSeries:8.4.0")
    implementation("me.mattstudios:triumph-msg-bukkit:2.2.4-SNAPSHOT")
}

// Disables the normal jar task
tasks.jar { enabled = false }

// And enables shadowJar task
artifacts.archives(tasks.shadowJar)

tasks.shadowJar {
    archiveFileName.set(rootProject.name + ".jar")
    val dependencyPackage = "${rootProject.group}.dependencies.${rootProject.name.toLowerCase()}"
//    relocate("org.jetbrains.exposed", "${dependencyPackage}.exposed")
//    relocate("kotlin", "${dependencyPackage}.kotlin")
    relocate("kotlinx", "${dependencyPackage}.kotlinx")
    relocate("org.slf4j", "${dependencyPackage}.slf4j")
    relocate("com.zaxxer.hikari", "${dependencyPackage}.hikari")
    relocate("org.jetbrains.annotations", "${dependencyPackage}.jetbrains.annotations")
    relocate("org.intellij", "${dependencyPackage}.jetbrains.intellij")
    relocate("toothpick", "${dependencyPackage}.toothpick")
    relocate("me.mattstudios.msg", "${dependencyPackage}.mf-msg")
    relocate("me.mattstudios.util", "${dependencyPackage}.mf-msg.util")
    relocate("javax.inject", "${dependencyPackage}.javax.inject")
    relocate("com.cryptomorin.xseries", "${dependencyPackage}.xseries")
    exclude("DebugProbesKt.bin")
    finalizedBy(tasks.getByName("copyJar"))
}

tasks.create("copyJar", Copy::class) {
    val jar = tasks.shadowJar.get().archiveFile.get().asFile
    val pluginFolder = file(rootDir).resolve("Server/plugins")
    if (pluginFolder.exists()) {
        from(jar).into(pluginFolder)
    }
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "1.8"
    targetCompatibility = "1.8"
    options.encoding = "UTF-8"
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict", "-Xjvm-default=all")
        jvmTarget = "1.8"
    }
}

tasks.processResources {
    val main_class = "${project.group}.${project.name.toLowerCase()}.${project.name}"
    expand("name" to project.name, "version" to project.version, "mainClass" to main_class)
}
