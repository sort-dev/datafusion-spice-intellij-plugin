pluginManagement {
    includeBuild("build-logic")
}

val supportCandidate = providers.gradleProperty("qualification.brikkSupportVersion")
    .orElse(providers.environmentVariable("BRIKK_SUPPORT_CANDIDATE")).orNull
if (supportCandidate != null) {
    require(supportCandidate.matches(Regex("[0-9A-Za-z.+-]+-SNAPSHOT"))) {
        "Owner-authorized local qualification accepts only explicit SNAPSHOT coordinates"
    }
    require(providers.gradleProperty("release").orNull != "true") {
        "A local qualification snapshot cannot be used for a release build"
    }
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("build-logic/gradle/libs.versions.toml"))
            version("brikk-sql-core", "1.0.1")
            version("brikk-sql-verify", "1.0.1")
            version("brikk-sql-intellij-support", supportCandidate ?: "1.0.1")
        }
    }
}

rootProject.name = "datafusion-spice-intellij-plugin"
