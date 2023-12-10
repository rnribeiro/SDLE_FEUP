plugins {
    id("java")
    application
}

application {
    applicationDefaultJvmArgs = listOf("-Dfile.encoding=UTF-8")

    // Configuration for Client
    tasks.create<JavaExec>("client") {
        mainClass.set("org.WishCloud.Client.Client")
        classpath = sourceSets["main"].runtimeClasspath
        standardInput = System.`in`
    }

    // Configuration for Cloud
    tasks.create<JavaExec>("cloud") {
        mainClass.set("org.WishCloud.Cloud.Cloud")
        classpath = sourceSets["main"].runtimeClasspath
    }

    // Configuration for Server Manager
    tasks.create<JavaExec>("server") {
        mainClass.set("org.WishCloud.Cloud.ServerManager")
        classpath = sourceSets["main"].runtimeClasspath
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.xerial:sqlite-jdbc:3.44.0.0")
    implementation("org.slf4j:slf4j-simple:1.7.36")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.0")
    implementation("com.fasterxml.jackson.core:jackson-core:2.16.0")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.16.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.9.1")
}

tasks.test {
    useJUnitPlatform()
}
