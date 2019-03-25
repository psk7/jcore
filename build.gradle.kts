import org.jetbrains.kotlin.gradle.tasks.*

plugins {
    base
    kotlin("jvm") version "1.3.21"
    //application
    //distribution
    maven
}

group = "pvt.psk"
version = "1.0.1-rc1"

repositories {
    mavenCentral()
    jcenter()
}

tasks.named<Upload>("uploadArchives") {
    repositories.withGroovyBuilder {
        "mavenDeployer" {
            "repository"("url" to "file:build/m2")
        }
    }
}

//application.mainClassName = "pvt.psk"

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
    implementation("io.ktor:ktor-network:1.1.3")
    implementation("joda-time:joda-time:2.10.1")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
    //kotlinOptions.suppressWarnings = true
    kotlinOptions.freeCompilerArgs = listOf("-Xuse-experimental=io.ktor.util.KtorExperimentalAPI", "-Xuse-experimental=kotlinx.coroutines.ExperimentalCoroutinesApi")
}

tasks.create<JavaExec>("runcon") {
    main = "pvt.psk.contest.MainKt"
    classpath += sourceSets["contest"].runtimeClasspath
    classpath += sourceSets["main"].runtimeClasspath
}

tasks.register("checkjvm") { 
    doLast { 
        if(!JavaVersion.current().isJava8())
          throw IllegalStateException("ERROR: Java 8 required but Java ${JavaVersion.current()} found. Change your JAVA_HOME environment variable.")
    }
}

//tasks["compileKotlin"].dependsOn += "checkjvm"

tasks {
    // Use the built-in JUnit support of Gradle.
    "test"(Test::class) {
        useJUnitPlatform()
    }
}
