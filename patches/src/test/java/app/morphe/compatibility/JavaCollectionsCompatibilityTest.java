package app.morphe.compatibility;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import static org.junit.Assert.*;

public class JavaCollectionsCompatibilityTest {
    private static final Set<String> NEW_LIST_METHODS = Set.of(
            "getFirst", "getLast", "removeFirst", "removeLast", "addFirst", "addLast", "reversed");

    @Test
    public void patchBytecodeDoesNotDependOnAndroid15SequencedCollections() throws Exception {
        String resource = "app/morphe/util/FreeRegisterProvider.class";
        URL entry = getClass().getClassLoader().getResource(resource);
        assertNotNull("Compiled patch classes must be available", entry);
        Path root = Path.of(entry.toURI());
        for (String ignored : resource.split("/")) root = root.getParent();
        List<String> unsupported = new ArrayList<>();
        int checked = 0;
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path path : paths.filter(p -> p.toString().endsWith(".class")).toList()) {
                checked++;
                try (InputStream input = Files.newInputStream(path)) {
                    ClassReader reader = new ClassReader(input);
                    reader.accept(new ClassVisitor(Opcodes.ASM9) {
                        @Override
                        public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                            if (descriptor.contains("Ljava/util/Sequenced")) unsupported.add(reader.getClassName() + "." + name);
                            return null;
                        }

                        @Override
                        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                            return new MethodVisitor(Opcodes.ASM9) {
                                @Override
                                public void visitMethodInsn(int opcode, String owner, String calledName, String calledDescriptor, boolean isInterface) {
                                    boolean newListApi = (owner.equals("java/util/List") || owner.equals("java/util/ArrayList"))
                                            && NEW_LIST_METHODS.contains(calledName);
                                    if (newListApi || owner.startsWith("java/util/Sequenced")) {
                                        unsupported.add(reader.getClassName() + "." + name + " -> " + owner + "." + calledName);
                                    }
                                }
                            };
                        }
                    }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                }
            }
        }
        assertTrue("Expected to inspect production patch classes", checked > 10);
        assertTrue("Android 14 and earlier cannot load these calls: " + unsupported, unsupported.isEmpty());
    }
}
