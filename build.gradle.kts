pluginModule=boomman

modules {
    java
    spigot
}

spigot {
    main = "cn.popcraft.boomman.BoomMan"
    name = "BoomMan"
    version = "1.0.0"
    apiVersion = "1.17"
    depend = listOf("CoreProtect")
}

repositories {
    mavenCentral()
    spigotmc()
    jitpack()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://repo.codemc.io/repository/maven-public/")
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
