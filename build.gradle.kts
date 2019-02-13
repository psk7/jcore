import org.jetbrains.kotlin.gradle.tasks.*

plugins {
    base
    kotlin("jvm") version "1.3.21"
    application
    distribution
}

group = "pvt.psk"
version = "0.1-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
}

application.mainClassName = "pvt.psk"

configurations.create("contestCompile") {
    extendsFrom(configurations.implementation.get())
}

sourceSets.create("contest") {
    compileClasspath += sourceSets["main"].output
    runtimeClasspath += sourceSets["main"].output
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.4.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.4.0")
    //testRuntime("org.junit.platform:junit-platform-console:1.2.0")

    implementation(kotlin("stdlib-jdk8"))
    implementation("io.ktor:ktor-network:1.1.2")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.create<JavaExec>("runcon") {
    main = "pvt.psk.contest.MainKt"
    classpath += sourceSets["contest"].runtimeClasspath
    classpath += sourceSets["main"].runtimeClasspath
}

tasks {
    // Use the built-in JUnit support of Gradle.
    "test"(Test::class) {
        useJUnitPlatform()
    }
}