plugins {
    id("dev.sort.sql-dialect-plugin")
}

group = "dev.sort.datafusionspice"
version = "0.1.0"

sqlDialectPlugin {
    kotlinTests = false
    embeddedLibraries = with(libs.versions) {
        setOf(
            "brikk-sql-jvmMain-${brikk.sql.core.get()}.jar",
            "brikk-sql-metadata-jvmMain-${brikk.sql.core.get()}.jar",
            "brikk-sql-verify-api-${brikk.sql.verify.get()}.jar",
            "brikk-sql-intellij-support-${brikk.sql.intellij.support.get()}.jar",
            "brikk-sql-intellij-jdbc-support-${brikk.sql.intellij.support.get()}.jar",
        )
    }
    requiredNotices = listOf("brikk-sql-intellij-support", "brikk-sql-metadata-jvm", "Apache License 2.0")
}

dependencies {
    implementation(libs.brikk.sql.jvm) {
        version { strictly(libs.versions.brikk.sql.core.get()) }
    }
    implementation(libs.brikk.sql.metadata.jvm) {
        version { strictly(libs.versions.brikk.sql.core.get()) }
    }
    implementation(libs.brikk.sql.intellij.support) {
        version { strictly(libs.versions.brikk.sql.intellij.support.get()) }
    }
    implementation(libs.brikk.sql.intellij.jdbc.support)
    constraints {
        implementation("dev.brikk.house:brikk-sql-verify-api") {
            version { strictly(libs.versions.brikk.sql.verify.get()) }
        }
    }
    testImplementation(libs.brikk.sql.intellij.test.support)
    // Same single shaded JAR Database Tools downloads; never a plugin runtime dependency.
    testImplementation("org.apache.arrow:flight-sql-jdbc-driver:19.0.0") {
        isTransitive = false
    }
}

val flightDriverClasspath = configurations.named("testRuntimeClasspath")
tasks.withType<Test>().configureEach {
    jvmArgumentProviders.add(org.gradle.process.CommandLineArgumentProvider {
        val driver = flightDriverClasspath.get().files.single { it.name == "flight-sql-jdbc-driver-19.0.0.jar" }
        val probeClasspath = layout.buildDirectory.file("cache-probe/$name-classpath.txt").get().asFile
        probeClasspath.parentFile.mkdirs()
        probeClasspath.writeText(classpath.asPath)
        listOf("-Dtest.flightDriverJar=${driver.absolutePath}", "-Dtest.cacheProbeClasspathFile=${probeClasspath.absolutePath}")
    })
    val liveUrl = providers.environmentVariable("BRIKK_FLIGHT_TEST_URL").orNull
    if (liveUrl == null) {
        exclude("**/live/**")
    } else {
        if (!providers.environmentVariable("BRIKK_FLIGHT_TEST_TLS_ROOT").isPresent) {
            exclude("**/live/FlightSqlSecurityTest*")
        }
        systemProperty("flight.test.url", liveUrl)
        systemProperty("flight.test.product", providers.environmentVariable("BRIKK_FLIGHT_TEST_PRODUCT").get())
        systemProperty("flight.test.output", layout.buildDirectory.dir("readiness/live").get().asFile.absolutePath)
        jvmArgs("--add-opens=java.base/java.nio=ALL-UNNAMED", "--sun-misc-unsafe-memory-access=allow")
    }
}
