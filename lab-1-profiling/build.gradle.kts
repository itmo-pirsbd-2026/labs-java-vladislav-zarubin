plugins {
    java
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
}

val jmh by sourceSets.creating {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    testRuntimeOnly(libs.junit.platform.launcher)

    add(jmh.implementationConfigurationName, libs.jmh.core)
    add(jmh.annotationProcessorConfigurationName, libs.jmh.generator.annprocess)
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<JavaExec>("jmh") {
    group = "benchmark"
    description = "Runs JMH benchmarks. Pass JMH options with --args."
    javaLauncher.set(javaToolchains.launcherFor(java.toolchain))
    classpath = jmh.runtimeClasspath
    mainClass.set("org.openjdk.jmh.Main")
    workingDir = rootProject.projectDir
}
