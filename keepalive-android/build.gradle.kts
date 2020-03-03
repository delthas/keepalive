plugins {
    kotlin("jvm")
	maven
    `maven-publish`
}

dependencies {
    implementation(project(":"))
    implementation("net.java.dev.jna:jna:5.5.0@aar") {
        isTransitive = true
    }
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
