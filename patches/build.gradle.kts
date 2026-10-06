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
    testImplementation(libs.junit)
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
                "app/morphe/patches/youtube/layout/hide/settingsmenu/HideSettingsMenuFilterPatch.kt",
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
    named<JavaCompile>("compileTestJava") {
        options.release.set(17)
    }
    jar {
        exclude("app/morphe/generator")
    }
    register<JavaExec>("generatePatchesList") {
        description = "Build patch with patch list"

        dependsOn("buildAndroid")

        // The generator needs Kotlin locally; the Android bundle must not package it.
        classpath = sourceSets["main"].output + configurations["compileClasspath"]
        mainClass.set("app.morphe.generator.MainKt")
        args(layout.buildDirectory.file("libs/patches-${project.version}.mpp").get().asFile.absolutePath)
    }
    // Used by gradle-semantic-release-plugin.
    publish {
        dependsOn("generatePatchesList")
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs = listOf("-Xcontext-parameters")
    }
}
