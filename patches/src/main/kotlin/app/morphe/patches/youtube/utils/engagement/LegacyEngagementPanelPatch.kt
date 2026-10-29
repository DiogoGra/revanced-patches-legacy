package app.morphe.patches.youtube.utils.engagement

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patcher.util.proxy.mutableTypes.MutableField.Companion.toMutable
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.youtube.utils.extension.Constants.UTILS_PATH
import app.morphe.patches.youtube.utils.extension.sharedExtensionPatch
import app.morphe.util.findFreeRegister
import app.morphe.util.findMutableClassOrThrow
import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.fingerprint.methodOrThrow
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.builder.BuilderInstruction
import com.android.tools.smali.dexlib2.builder.Label
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter
import com.android.tools.smali.dexlib2.immutable.ImmutableField

private const val ROOT = "$UTILS_PATH/LegacyEngagementPanelPatch"
private const val HELPER = "$ROOT;"
private const val COMMAND = "$ROOT\$NativeCommand;"
private const val MODEL = "$ROOT\$NativeModel;"
private const val PANEL = "$ROOT\$NativePanel;"
private const val TRANSCRIPT = "$ROOT\$NativeTranscript;"
private const val CONTROLLER = "$ROOT\$NativeController;"
private const val LOADER = "$ROOT\$NativeLoader;"
private const val CALLBACK = "$ROOT\$FetchCallback;"

private val legacyUnresolvedPanelCommand = legacyFingerprint(
    name = "legacyUnresolvedPanelCommand",
    returnType = "V",
    parameters = listOf("Laoiz;", "Ljava/util/Map;"),
    strings = listOf("Unknown command not resolved"),
    customFingerprint = { method, clazz -> clazz.type == "Lztq;" && method.name == "g" },
)

internal fun legacyPanelCommandSite(method: Method): Pair<Int, Int> {
    val instructions = method.implementation!!.instructions.toList()
    val logging = instructions.indexOfFirst {
        it.getReference<StringReference>()?.string == "Unknown command not resolved"
    }
    check(logging >= 0) { "Legacy command logging site is missing" }
    // R8 overwrites the parameter with its serialized String before loading the log prefix.
    val index = (0 until logging).lastOrNull {
        val reference = instructions[it].getReference<MethodReference>()
        reference?.definingClass in setOf("Laoiz;", "Lamns;", "Ljava/lang/Object;") && reference?.name == "toString"
                && reference.parameterTypes.isEmpty() && reference.returnType == "Ljava/lang/String;"
    } ?: error("Legacy command object is not available before logging")
    val invocation = instructions[index] as FiveRegisterInstruction
    check(invocation.registerCount == 1)
    return index to invocation.registerC
}

internal fun moveLegacyPanelEntryLabels(original: BuilderInstruction, inserted: BuilderInstruction, incoming: List<Label>) {
    // Existing branches must enter the adapter; its explicit fallback label stays on the native code.
    incoming.forEach { label ->
        check(original.location.labels.remove(label))
        inserted.location.labels.add(label)
    }
}

private fun MutableClass.bridge(name: String, result: String, parameters: List<String> = emptyList(), registers: Int = 3, body: String) {
    check(methods.none { it.name == name && it.parameterTypes.map { p -> p.toString() } == parameters }) {
        "Legacy panel bridge already exists: $name"
    }
    methods.add(ImmutableMethod(type, name, parameters.map { ImmutableMethodParameter(it, null, null) },
        result, AccessFlags.PUBLIC.value or AccessFlags.FINAL.value, null, null,
        MutableMethodImplementation(registers)).toMutable().apply { addInstructions(0, body) })
}

context(_: BytecodePatchContext)
private fun requireFields(type: String, vararg fields: Pair<String, String>): MutableClass =
    findMutableClassOrThrow(type).also { clazz ->
        for ((name, fieldType) in fields) check(clazz.fields.any { it.name == name && it.type == fieldType }) {
            "Unsupported legacy engagement panel field: $type->$name:$fieldType"
        }
    }

internal val legacyEngagementPanelPatch = bytecodePatch(description = "legacyEngagementPanelPatch") {
    dependsOn(sharedExtensionPatch)
    execute {
        // REA + DEX contracts of the supported 19.16.39 artifact. Fail closed on a different ABI.
        requireFields("Lamns;", "unknownFields" to "Lamqk;")
        requireFields("Lamqk;", "b" to "I", "c" to "[I", "d" to "[Ljava/lang/Object;")
        requireFields("Lamnk;", "instance" to "Lamns;")
        requireFields("Lamnn;", "l" to "Lamnf;")
        requireFields("Lamnq;", "b" to "Ljava/lang/Object;", "d" to "Lamnp;")
        val model = requireFields("Lapml;", "h" to "Lapmj;", "c" to "I", "e" to "I", "f" to "Ljava/lang/Object;")
        requireFields("Lapmj;", "b" to "I")
        val panel = requireFields("Lzlu;", "p" to "Larhu;")
        requireFields("Lathi;", "b" to "I", "c" to "Ljava/lang/String;", "d" to "Ljava/lang/String;")
        requireFields("Lathg;", "b" to "I", "f" to "Lathi;")
        requireFields("Lathk;", "b" to "I", "c" to "Ljava/lang/Object;")
        val dispatcher = legacyUnresolvedPanelCommand.methodOrThrow()
        val controller = findMutableClassOrThrow("Lzmy;")
        val command = findMutableClassOrThrow("Laoiz;")
        val loader = findMutableClassOrThrow("Lzbo;")

        model.interfaces.add(MODEL)
        model.bridge("rvxPanelIdentifier", "Ljava/lang/String;", body = """
            invoke-static {p0}, Laclx;->eI(Lapml;)Ljava/lang/String;
            move-result-object v0
            return-object v0
        """)
        model.bridge("rvxPanelBodyType", "I", body = """
            iget-object v0, p0, Lapml;->h:Lapmj;
            if-eqz v0, :empty
            iget v0, v0, Lapmj;->b:I
            return v0
            :empty
            const/4 v0, 0x0
            return v0
        """)
        model.bridge("rvxPanelWithContent", MODEL, listOf(MODEL), 5, """
            invoke-virtual {p0}, Lamns;->toBuilder()Lamnk;
            move-result-object v0
            invoke-virtual {v0}, Lamnk;->copyOnWrite()V
            iget-object v1, v0, Lamnk;->instance:Lamns;
            check-cast v1, Lapml;
            check-cast p1, Lapml;
            iget-object v2, p1, Lapml;->h:Lapmj;
            iput-object v2, v1, Lapml;->h:Lapmj;
            iget v2, v1, Lapml;->c:I
            or-int/lit8 v2, v2, 0x4
            iput v2, v1, Lapml;->c:I
            invoke-virtual {v0}, Lamnk;->build()Lamns;
            move-result-object v0
            check-cast v0, Lapml;
            return-object v0
        """)
        model.bridge("rvxPanelWithIdentifier", MODEL, listOf("Ljava/lang/String;"), 5, """
            invoke-virtual {p0}, Lamns;->toBuilder()Lamnk;
            move-result-object v0
            invoke-virtual {v0}, Lamnk;->copyOnWrite()V
            iget-object v1, v0, Lamnk;->instance:Lamns;
            check-cast v1, Lapml;
            const/4 v2, 0x1
            iput v2, v1, Lapml;->e:I
            iput-object p1, v1, Lapml;->f:Ljava/lang/Object;
            invoke-virtual {v0}, Lamnk;->build()Lamns;
            move-result-object v0
            check-cast v0, Lapml;
            return-object v0
        """)

        command.interfaces.add(COMMAND)
        command.bridge("rvxPanelUpdateData", "[B", registers = 6, body = """
            iget-object v0, p0, Lamns;->unknownFields:Lamqk;
            if-eqz v0, :empty
            iget v1, v0, Lamqk;->b:I
            iget-object v2, v0, Lamqk;->c:[I
            iget-object v3, v0, Lamqk;->d:[Ljava/lang/Object;
            invoke-static {v1, v2, v3}, $HELPER->findUpdateValue(I[I[Ljava/lang/Object;)Ljava/lang/Object;
            move-result-object v0
            instance-of v1, v0, Lammn;
            if-eqz v1, :empty
            check-cast v0, Lammn;
            invoke-virtual {v0}, Lammn;->H()[B
            move-result-object v0
            return-object v0
            :empty
            const/4 v0, 0x0
            return-object v0
        """)
        val endpoints = listOf(
            Triple("ShowEngagementPanelEndpoint", "showEngagementPanelEndpoint", "invoke-static {v1}, Laclx;->eJ(%s)Ljava/lang/String;"),
            Triple("ShowEngagementPanelNavigationEndpoint", "showEngagementPanelNavigationEndpoint", "iget-object v0, v1, %s->c:Ljava/lang/String;\nreturn-object v0"),
            Triple("ToggleEngagementPanelCommand", "toggleEngagementPanelCommand", "invoke-static {v1}, Laclx;->eK(%s)Ljava/lang/String;"),
        )
        val endpointReader = buildString {
            endpoints.forEachIndexed { index, (name, extension, readId) ->
                val type = "Lcom/google/protos/youtube/api/innertube/${name}OuterClass\$$name;"
                append("""
                    sget-object v0, $type->$extension:Lamnq;
                    invoke-virtual {p0, v0}, Lamnn;->d(Lamnq;)V
                    iget-object v1, p0, Lamnn;->l:Lamnf;
                    iget-object v2, v0, Lamnq;->d:Lamnp;
                    invoke-virtual {v1, v2}, Lamnf;->o(Lamnp;)Z
                    move-result v3
                    if-eqz v3, :next_$index
                    invoke-virtual {v1, v2}, Lamnf;->l(Lamnp;)Ljava/lang/Object;
                    move-result-object v1
                    if-nez v1, :present_$index
                    iget-object v1, v0, Lamnq;->b:Ljava/lang/Object;
                    goto :typed_$index
                    :present_$index
                    invoke-virtual {v0, v1}, Lamnq;->c(Ljava/lang/Object;)Ljava/lang/Object;
                    move-result-object v1
                    :typed_$index
                    check-cast v1, $type
                """.trimIndent()).append('\n')
                append(readId.format(type)).append('\n')
                if (index != 1) append("move-result-object v0\nreturn-object v0\n")
                append(":next_$index\nnop\n")
            }
            append("const/4 v0, 0x0\nreturn-object v0")
        }
        command.bridge("rvxPanelShowId", "Ljava/lang/String;", registers = 5, body = endpointReader)

        panel.interfaces.add(PANEL)
        panel.bridge("rvxPanelModel", MODEL, body = """
            invoke-interface {p0}, Lzmm;->H()Lapml;
            move-result-object v0
            return-object v0
        """)
        panel.bridge("rvxPanelIsTranscript", "Z", body = """
            instance-of v0, p0, Lmpd;
            return v0
        """)
        panel.bridge("rvxPanelUpdate", "V", listOf(MODEL), 3, """
            check-cast p1, Lapml;
            const/4 v0, 0x0
            invoke-interface {p0, p1, v0, v0}, Lzmm;->R(Lapml;Laoiz;Z)V
            return-void
        """)

        controller.interfaces.add(CONTROLLER)
        controller.bridge("rvxPanelActiveId", "Ljava/lang/String;", body = """
            invoke-virtual {p0}, Lzmy;->g()Ljava/lang/String;
            move-result-object v0
            return-object v0
        """)
        controller.bridge("rvxPanelGet", PANEL, listOf("Ljava/lang/String;"), 4, """
            invoke-virtual {p0, p1}, Lzmy;->a(Ljava/lang/String;)Lzmm;
            move-result-object v0
            instance-of v1, v0, $PANEL
            if-nez v1, :found
            const/4 v0, 0x0
            return-object v0
            :found
            check-cast v0, $PANEL
            return-object v0
        """)
        controller.bridge("rvxPanelRegister", "V", listOf(MODEL), 2, """
            check-cast p1, Lapml;
            invoke-virtual {p0, p1}, Lzmy;->u(Lapml;)V
            return-void
        """)
        controller.bridge("rvxPanelRebuild", "V", listOf(MODEL, PANEL, COMMAND), 10, """
            move-object v0, p0
            move-object v1, p1
            check-cast v1, Lapml;
            check-cast p2, Lzlu;
            iget-object v2, p2, Lzlu;->p:Larhu;
            const/4 v3, 0x1
            const/4 v4, 0x0
            const/4 v5, 0x1
            invoke-virtual/range {v0 .. v5}, Lzmy;->v(Lapml;Larhu;ZLaoiz;Z)V
            check-cast p3, Laoiz;
            invoke-virtual {p0, p3, v4}, Lzmy;->c(Laoiz;Lzmk;)Lzmm;
            return-void
        """)
        controller.methods.single { it.name == "I" && it.parameterTypes.size == 4 }.addInstructions(0, """
            invoke-static/range {p0 .. p1}, $HELPER->onPanelOpening(Ljava/lang/Object;Ljava/lang/Object;)V
        """)

        findMutableClassOrThrow("Lzlv;").methods.single {
            it.name == "N" && it.returnType == "Z" && it.parameterTypes.map { p -> p.toString() } == listOf("Lapml;", "Laoiz;")
        }.apply {
            val parametersStart = implementation!!.registerCount - 3
            val register = findFreeRegister(0, parametersStart, parametersStart + 1, parametersStart + 2)
            addInstructionsWithLabels(0, """
                invoke-static/range {p1 .. p1}, $HELPER->shouldUseNativeFactory(Ljava/lang/Object;)Z
                move-result v$register
                if-eqz v$register, :original
                const/4 v$register, 0x0
                return v$register
            """, ExternalLabel("original", getInstruction(0)))
        }

        val transcript = requireFields("Lmpd;", "c" to "Lakid;", "l" to "Lazql;")
        requireFields("Lavln;", "c" to "Lavlm;")
        requireFields("Lavlm;", "b" to "I", "f" to "Ljava/lang/String;")
        transcript.interfaces.add(TRANSCRIPT)
        val observed = "Lmpd;->rvxTranscriptEntitiesObserved:Z"
        check(transcript.fields.none { it.name == "rvxTranscriptEntitiesObserved" })
        transcript.fields.add(ImmutableField("Lmpd;", "rvxTranscriptEntitiesObserved", "Z",
            AccessFlags.PRIVATE.value, null, null, null).toMutable())
        transcript.bridge("rvxPanelWatchTranscriptEntities", "Z", listOf(MODEL), 5, """
            if-eqz p1, :unsupported
            iget-object v0, p0, Lzlu;->o:Lapml;
            if-eqz v0, :unsupported
            iget-object v0, v0, Lapml;->h:Lapmj;
            if-eqz v0, :unsupported
            check-cast p1, Lapml;
            iget-object v1, p1, Lapml;->h:Lapmj;
            if-ne v0, v1, :unsupported
            iget v1, v0, Lapmj;->b:I
            const v2, 154115041
            if-ne v1, v2, :unsupported
            iget-object v0, v0, Lapmj;->c:Ljava/lang/Object;
            check-cast v0, Lavln;
            iget-object v0, v0, Lavln;->c:Lavlm;
            if-eqz v0, :unsupported
            iget v0, v0, Lavlm;->b:I
            and-int/lit8 v0, v0, 0x8
            if-eqz v0, :unsupported
            iget-boolean v0, p0, $observed
            if-nez v0, :observing
            invoke-direct {p0}, Lmpd;->b()V
            :observing
            const/4 v0, 0x1
            return v0
            :unsupported
            const/4 v0, 0x0
            return v0
        """)
        transcript.methods.single { it.name == "b" && it.parameterTypes.isEmpty() && it.returnType == "V" }.apply {
            val register = findFreeRegister(0, implementation!!.registerCount - 1)
            addInstructions(0, """
                const/4 v$register, 0x1
                iput-boolean v$register, p0, $observed
            """)
        }
        transcript.methods.single {
            it.name == "o" && it.returnType == "V" && it.parameterTypes.isEmpty()
        }.apply {
            check(implementation!!.tryBlocks.isEmpty()) { "Legacy transcript cleanup boundaries changed" }
            val transaction = indexOfFirstInstructionOrThrow {
                getReference<FieldReference>()?.let { it.definingClass == "Lmpd;" && it.name == "a" && it.type == "Laaak;" } == true
            }
            val releaseView = indexOfFirstInstructionOrThrow {
                getReference<FieldReference>()?.let { it.definingClass == "Lmpd;" && it.name == "j" && it.type == "Lznt;" } == true
            }
            check(transaction < releaseView)
            val original = getInstruction<BuilderInstruction>(transaction)
            val incoming = original.location.labels.toList()
            val parametersStart = implementation!!.registerCount - 1
            val register = findFreeRegister(transaction, parametersStart)
            addInstructionsWithLabels(transaction, """
                invoke-static/range {p0 .. p0}, $HELPER->shouldSkipTranscriptEntityCleanup(Ljava/lang/Object;)Z
                move-result v$register
                if-nez v$register, :release_view
            """, ExternalLabel("release_view", getInstruction(releaseView)))
            moveLegacyPanelEntryLabels(original, getInstruction(transaction), incoming)
            val registerAtEntry = findFreeRegister(0, implementation!!.registerCount - 1)
            addInstructions(0, """
                const/4 v$registerAtEntry, 0x0
                iput-boolean v$registerAtEntry, p0, $observed
            """)
        }

        loader.interfaces.add(LOADER)
        loader.bridge("rvxPanelFetch", "V", listOf("Ljava/lang/String;", "Ljava/lang/String;", CALLBACK), 12, """
            sget-object v0, Lathi;->a:Lathi;
            invoke-virtual {v0}, Lamns;->toBuilder()Lamnk;
            move-result-object v0
            invoke-virtual {v0}, Lamnk;->copyOnWrite()V
            iget-object v1, v0, Lamnk;->instance:Lamns;
            check-cast v1, Lathi;
            const/4 v2, 0x3
            iput v2, v1, Lathi;->b:I
            iput-object p1, v1, Lathi;->c:Ljava/lang/String;
            iput-object p2, v1, Lathi;->d:Ljava/lang/String;
            invoke-virtual {v0}, Lamnk;->build()Lamns;
            move-result-object v0
            check-cast v0, Lathi;
            sget-object v1, Lathg;->a:Lathg;
            invoke-virtual {v1}, Lamns;->toBuilder()Lamnk;
            move-result-object v1
            invoke-virtual {v1}, Lamnk;->copyOnWrite()V
            iget-object v2, v1, Lamnk;->instance:Lamns;
            check-cast v2, Lathg;
            iput-object v0, v2, Lathg;->f:Lathi;
            const/4 v3, 0x2
            iput v3, v2, Lathg;->b:I
            invoke-virtual {v1}, Lamnk;->build()Lamns;
            move-result-object v1
            check-cast v1, Lathg;
            move-object v0, p0
            move-object v2, p3
            check-cast v2, Lzox;
            const/4 v3, 0x0
            new-array v3, v3, [B
            invoke-virtual {v0, v1, v2, v3}, Lzbo;->h(Lathg;Lzox;[B)V
            return-void
        """)
        loader.methods.single {
            it.name == "<init>" && it.parameterTypes.map { p -> p.toString() } ==
                    listOf("Lbatk;", "Ljava/util/concurrent/Executor;", "Lztw;", "Lbmx;")
        }.apply {
            val index = indexOfFirstInstructionOrThrow { opcode == Opcode.RETURN_VOID }
            addInstructions(index, "invoke-static/range {p0 .. p0}, $HELPER->onLoaderAvailable(Ljava/lang/Object;)V")
        }

        findMutableClassOrThrow(CALLBACK).apply {
            interfaces.add("Lzox;")
            bridge("a", "V", listOf("Lathk;"), 2, "return-void")
            bridge("d", "V", registers = 1, body = """
                invoke-virtual {p0}, $CALLBACK->onFailure()V
                return-void
            """)
            bridge("e", "V", listOf("Lathk;", "[B"), 5, """
                iget v0, p1, Lathk;->b:I
                const v1, 138681066
                if-ne v0, v1, :failed
                iget-object v0, p1, Lathk;->c:Ljava/lang/Object;
                instance-of v1, v0, Lapml;
                if-eqz v1, :failed
                check-cast v0, Lapml;
                invoke-virtual {p0, v0}, $CALLBACK->onSuccess($MODEL)V
                return-void
                :failed
                invoke-virtual {p0}, $CALLBACK->onFailure()V
                return-void
            """)
        }

        dispatcher.apply {
            val (index, commandRegister) = legacyPanelCommandSite(this)
            check(implementation!!.tryBlocks.isEmpty()) { "Legacy dispatcher exception boundaries changed" }
            val original = getInstruction<BuilderInstruction>(index)
            val incoming = original.location.labels.toList()
            val parametersStart = implementation!!.registerCount - 3
            val register = findFreeRegister(index, parametersStart, parametersStart + 1, parametersStart + 2, commandRegister)
            addInstructionsWithLabels(index, """
                invoke-static/range {v$commandRegister .. v$commandRegister}, $HELPER->tryHandle(Ljava/lang/Object;)Z
                move-result v$register
                if-eqz v$register, :original
                return-void
            """, ExternalLabel("original", getInstruction(index)))
            moveLegacyPanelEntryLabels(original, getInstruction(index), incoming)
        }
    }
}
