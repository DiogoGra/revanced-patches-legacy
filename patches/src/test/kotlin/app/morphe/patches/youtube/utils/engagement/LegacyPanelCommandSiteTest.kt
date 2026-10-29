package app.morphe.patches.youtube.utils.engagement

import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter
import com.android.tools.smali.dexlib2.immutable.instruction.*
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableStringReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction10x
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21t

class LegacyPanelCommandSiteTest {
    @Test
    fun nativeBranchEntersTheAdapterButItsFallbackStillEntersOriginalCode() {
        val body = MutableMethodImplementation(1)
        body.addInstruction(BuilderInstruction10x(Opcode.NOP))
        body.addInstruction(BuilderInstruction10x(Opcode.RETURN_VOID))
        val target = body.newLabelForIndex(1)
        val branch = BuilderInstruction21t(Opcode.IF_EQZ, 0, target)
        body.addInstruction(1, branch)
        val original = body.instructions[2]
        val incoming = original.location.labels.toList()
        body.addInstruction(2, BuilderInstruction10x(Opcode.NOP))
        val inserted = body.instructions[2]
        val fallback = body.newLabelForIndex(3)
        moveLegacyPanelEntryLabels(original, inserted, incoming)
        assertSame(inserted.location, branch.target.location)
        assertSame(original.location, fallback.location)
    }

    @Test
    fun locatesTheCommandBeforeR8ReplacesItWithAString() {
        val method = ImmutableMethod(
            "Lztq;", "g",
            listOf(ImmutableMethodParameter("Laoiz;", null, null), ImmutableMethodParameter("Ljava/util/Map;", null, null)),
            "V", AccessFlags.PUBLIC.value, null, null,
            ImmutableMethodImplementation(5, listOf(
                ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 1, 3, 0, 0, 0, 0,
                    ImmutableMethodReference("Ljava/lang/Object;", "toString", emptyList(), "Ljava/lang/String;")),
                ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, 3),
                ImmutableInstruction21c(Opcode.CONST_STRING, 4, ImmutableStringReference("Unknown command not resolved")),
                ImmutableInstruction10x(Opcode.RETURN_VOID),
            ), null, null),
        )
        assertEquals(0 to 3, legacyPanelCommandSite(method))
    }

    @Test(expected = IllegalStateException::class)
    fun refusesAChangedNativeLoggingContract() {
        val method = ImmutableMethod("Lztq;", "g", emptyList(), "V", AccessFlags.PUBLIC.value,
            null, null, ImmutableMethodImplementation(1, listOf(ImmutableInstruction10x(Opcode.RETURN_VOID)), null, null))
        legacyPanelCommandSite(method)
    }
}
