package app.morphe.generator

import app.morphe.patcher.patch.loadPatchesFromJar
import java.io.File
import java.util.jar.JarFile

internal fun main(args: Array<String>) {
    val bundle = File(args.single())
    require(bundle.isFile) { "Patch bundle not found: $bundle" }
    val version = JarFile(bundle).use {
        requireNotNull(it.manifest.mainAttributes.getValue("Version")) {
            "Patch bundle has no version: $bundle"
        }
    }
    val loadedPatches = loadPatchesFromJar(setOf(bundle))
    arrayOf(JsonPatchesFileGenerator(), ReadMeFileGenerator()).forEach {
        it.generate(version, loadedPatches)
    }
}
