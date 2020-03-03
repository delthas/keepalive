import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.50"
    maven
    `maven-publish`
}

allprojects {
    group = "fr.delthas"
    version = "0.0.2-SNAPSHOT"

    repositories {
        jcenter()
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("net.java.dev.jna:jna:5.5.0")
    testImplementation("junit:junit:4.12")
    testImplementation("org.slf4j:slf4j-simple:1.7.25")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.test {
    testLogging {
        showStandardStreams = true
    }
}

tasks.register<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

tasks.register<Jar>("javadocJar") {
    archiveClassifier.set("javadoc")
    from(tasks.javadoc.get().destinationDir)
}

val deployerJars: Configuration by configurations.creating

dependencies {
    deployerJars("org.apache.maven.wagon:wagon-ftp:2.2")
}

tasks.named<Upload>("uploadArchives") {
    repositories.withGroovyBuilder {
        "mavenDeployer" {
            setProperty("configuration", deployerJars)
            "repository"("url" to "ftp://saucisseroyale.cc/public/maven_repo/") {
                "authentication"("userName" to project.properties["ftp.username"], "password" to project.properties["ftp.password"])
            }
        }
    }
}
