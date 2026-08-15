group = "app.morphe"

patches {
    about {
        name = "RVX Patches"
        description = "Patches for RVX"
        source = "https://github.com/DiogoGra/revanced-patches-legacy"
        author = "DiogoGra"
        contact = "https://github.com/DiogoGra/revanced-patches-legacy/issues"
        website = "https://rvxtranslate.vercel.app/"
        license = "GNU General Public License v3.0"
    }
}

dependencies {
    // Used by JsonGenerator.
    implementation(libs.gson)
}

configurations.named("runtimeClasspath") {
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
}

sourceSets {
    main {
        kotlin {
            exclude(
                "app/morphe/patches/music/**",
                "app/morphe/patches/reddit/**",
            )
        }
        resources {
            exclude(
                "music/**",
                "reddit/**",
            )
        }
    }
}

tasks {
    jar {
        exclude("app/morphe/generator")
    }
    register<JavaExec>("generatePatchesList") {
        description = "Build patch with patch list"

        dependsOn(build)

        classpath = sourceSets["main"].runtimeClasspath
        mainClass.set("app.morphe.generator.MainKt")
    }
    // Used by gradle-semantic-release-plugin.
    publish {
        dependsOn("generatePatchesList")
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs = listOf("-Xcontext-receivers")
    }
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/DiogoGra/revanced-patches-legacy")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
