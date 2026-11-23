group = "com.willfp"
version = rootProject.version

val ecoVersion = rootProject.findProperty("eco-version")

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")

    testImplementation("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    testImplementation("wiki.ecohub:eco:$ecoVersion")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("io.mockk:mockk:1.14.11")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.4")
}

// Mirror compileOnly onto tests.
configurations.testImplementation.get().extendsFrom(configurations.compileOnly.get())

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}
