import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar

plugins {
    java
}

group = providers.gradleProperty("group").get()
version = providers.gradleProperty("version").get()
description = "A flexible player mount plugin for Bukkit-compatible Minecraft servers"

val javaVersion = providers.gradleProperty("javaVersion").get().toInt()
val paperApiVersion = providers.gradleProperty("paperApiVersion").get()
val spigotApiVersion = providers.gradleProperty("spigotApiVersion").get()
val junitVersion = providers.gradleProperty("junitVersion").get()
val jetbrainsAnnotationsVersion = providers.gradleProperty("jetbrainsAnnotationsVersion").get()

val paperApiClasspath = configurations.create("paperApiClasspath") {
    description = "Paper API used to compile-check the portable plugin sources"
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    // Spigot is the canonical compile target because CraftBukkit does not publish a
    // current standalone API artifact. The separate Paper compile catches API divergence.
    compileOnly("org.spigotmc:spigot-api:$spigotApiVersion")
    add(paperApiClasspath.name, "io.papermc.paper:paper-api:$paperApiVersion")
    // Paper exposes JetBrains nullability annotations in signatures without declaring them.
    add(paperApiClasspath.name, "org.jetbrains:annotations:$jetbrainsAnnotationsVersion")

    testImplementation(platform("org.junit:junit-bom:$junitVersion"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(javaVersion)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(javaVersion)
    options.compilerArgs.add("-Xlint:unchecked")
}

tasks.named<JavaCompile>("compileJava") {
    description = "Compiles the plugin against the Spigot 26.2 compatibility baseline"
    options.compilerArgs.add("-Xlint:deprecation")
}

tasks.named<JavaCompile>("compileTestJava") {
    options.compilerArgs.add("-Xlint:deprecation")
}

val compilePaperCompatibilityJava = tasks.register<JavaCompile>("compilePaperCompatibilityJava") {
    group = "verification"
    description = "Compiles the same plugin sources against the Paper 26.2 API"

    source(sourceSets.main.get().java)
    classpath = paperApiClasspath
    destinationDirectory = layout.buildDirectory.dir("classes/java/paperCompatibility")
    // Paper deprecates String text bridges that remain necessary on Spigot and CraftBukkit.
    // Missing or removed Paper APIs still fail this task; only that advisory lint is suppressed.
    options.compilerArgs.add("-Xlint:-deprecation")
    javaCompiler = javaToolchains.compilerFor {
        languageVersion = JavaLanguageVersion.of(javaVersion)
    }
}

tasks.processResources {
    val pluginVersion = project.version.toString()

    filteringCharset = "UTF-8"
    inputs.property("version", pluginVersion)
    filesMatching("plugin.yml") {
        expand("version" to pluginVersion)
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.check {
    dependsOn(compilePaperCompatibilityJava)
}

tasks.jar {
    archiveBaseName.set("HorseMount")
    manifest {
        attributes(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
        )
    }
}

val versionedJar = tasks.named<Jar>("jar")

val stageDockerPlugin = tasks.register<Copy>("stageDockerPlugin") {
    group = "build"
    description = "Stages the verified plugin JAR for the local Docker Compose server"

    dependsOn(tasks.named("check"), versionedJar)
    from(versionedJar.flatMap { it.archiveFile })
    into(layout.buildDirectory.dir("docker/plugins"))
    rename { "HorseMount.jar" }
}

tasks.build {
    dependsOn(stageDockerPlugin)
}

tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}
