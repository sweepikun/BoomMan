plugins {
    `java-library`
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "cn.popcraft"
version = "1.0.0"

repositories {
    mavenCentral()
    maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") }
    maven { url = uri("https://repo.codemc.io/repository/maven-public/") }
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.17.1-R0.1-SNAPSHOT")
    compileOnly("net.coreprotect:coreprotect:5.5.6")
    implementation("org.bstats:bstats-bukkit:3.0.2")
    implementation("org.xerial:sqlite-jdbc:3.45.1.0")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(17)
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.shadowJar {
    archiveFileName.set("BoomMan-${version}.jar")
    minimize {
        exclude(dependency("org.spigotmc:spigot-api.*"))
        exclude(dependency("net.coreprotect:coreprotect.*"))
    }
}

tasks.register<Copy>("copyDeps") {
    from(configurations.compileClasspath)
    into("build/libs")
    include("spigot-api*.jar")
    include("coreprotect*.jar")
}
