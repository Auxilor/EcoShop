group = "com.willfp"
version = rootProject.version

val ecoVersion = rootProject.findProperty("eco-version")

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")

    implementation("com.willfp:ecomponent:1.5.0")

    testImplementation("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    testImplementation("com.willfp:eco:$ecoVersion")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("io.mockk:mockk:1.13.13")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.4")
}

// Mirror compileOnly (eco, libreforge injected by the libreforge gradle plugin) onto tests.
configurations.testImplementation.get().extendsFrom(configurations.compileOnly.get())

// libreforge metadata declares an unpublished runtime dependency; drop it for tests.
configurations.testRuntimeClasspath.get().exclude(group = "libreforge.core")

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}
