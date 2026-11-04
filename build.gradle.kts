plugins {
    id("dev.sort.sql-dialect-plugin")
}

group = "dev.sort.datafusionspice"
val supportCandidate = providers.gradleProperty("qualification.brikkSupportVersion")
    .orElse(providers.environmentVariable("BRIKK_SUPPORT_CANDIDATE"))
version = if (supportCandidate.isPresent) "0.1.0-qualification" else "0.1.0"

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
        // An external fixture is state, not a reproducible Gradle task input.
        // In particular both halves of the credential-restart lane must execute.
        outputs.upToDateWhen { false }
        outputs.cacheIf { false }
        if (!providers.environmentVariable("BRIKK_FLIGHT_TEST_TLS_ROOT").isPresent) {
            exclude("**/live/FlightSqlSecurityTest*")
            exclude("**/live/FlightTlsContractTest*")
        }
        if (providers.environmentVariable("BRIKK_FLIGHT_TEST_PRODUCT").orNull != "datafusion") exclude("**/live/FlightTlsContractTest*")
        if (providers.environmentVariable("BRIKK_FLIGHT_TEST_PRODUCT").orNull != "spice") exclude("**/live/FlightSqlSecurityTest*")
        if (providers.environmentVariable("BRIKK_FLIGHT_TEST_LIFECYCLE_PROBE").orNull != "true") exclude("**/live/SharedFlightCatalogRaceTest*")
        if (providers.environmentVariable("BRIKK_FLIGHT_TEST_CANCELLATION_PROBE").orNull != "true") exclude("**/live/FlightCancellationTest*")
        systemProperty("flight.test.url", liveUrl)
        systemProperty("flight.test.product", providers.environmentVariable("BRIKK_FLIGHT_TEST_PRODUCT").get())
        systemProperty("flight.test.output", layout.buildDirectory.dir("readiness/live").get().asFile.absolutePath)
        jvmArgs("--add-opens=java.base/java.nio=ALL-UNNAMED", "--sun-misc-unsafe-memory-access=allow")
    }
}

// Public IntelliJ testing DSL, scoped to an explicitly supplied DataGrip install.
// The shared IU lanes request Java/Kotlin bundled hosts even in absent mode;
// this SQL-only consumer lane requests only plugins DataGrip actually bundles.
providers.gradleProperty("test.dataGripIde").orNull?.let { installation ->
    extensions.configure<org.jetbrains.intellij.platform.gradle.extensions.IntelliJPlatformTestingExtension> {
        testIde.register("testDataGrip") {
            type.set(org.jetbrains.intellij.platform.gradle.IntelliJPlatformType.DataGrip)
            localPath.set(file(installation))
            testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
            plugins {
                bundledPlugins(listOf("com.intellij.database", "com.intellij.modules.json", "Git4Idea"))
            }
            task {
                useJUnit()
                systemProperty("java.awt.headless", "true")
                systemProperty("test.kotlin", "absent")
                systemProperty("test.pluginIsolation", "false")
                systemProperty("test.siblingPlugins", "")
                exclude("**/brikk/**", "**/PluginIsolationTest*")
                jvmArgumentProviders.add(org.gradle.process.CommandLineArgumentProvider {
                    listOf("-Didea.load.plugins.id=com.intellij.database,Git4Idea,dev.sort.datafusion-spice-intellij-plugin")
                })
            }
        }
    }
}
