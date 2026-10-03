package dev.hoyin1600p.vault_render_optimization.compat.flywheelshader;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;

class OculusCompatContractTest {
    private static List<byte[]> vroCompatClasses() throws IOException, URISyntaxException {
        Path root = Path.of(OculusCompatContract.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Path compat = root.resolve("dev/hoyin1600p/vault_render_optimization/compat/flywheelshader");
        List<byte[]> classes = new ArrayList<>();
        try (Stream<Path> files = Files.walk(compat)) {
            for (Path file : (Iterable<Path>) files.filter(path -> path.toString().endsWith(".class"))::iterator) {
                classes.add(Files.readAllBytes(file));
            }
        }
        assertTrue(classes.size() > 40, "compat classes were not found under " + compat);
        return classes;
    }

    private static Function<String, byte[]> jar(String path) {
        return name -> {
            try (var zip = new ZipFile(path)) {
                var entry = zip.getEntry(name + ".class");
                if (entry == null) return null;
                try (var stream = zip.getInputStream(entry)) { return stream.readAllBytes(); }
            } catch (IOException failure) { throw new UncheckedIOException(failure); }
        };
    }

    private static List<String> contractJars() {
        String jars = System.getProperty("vro.oculus.contractJars", "");
        return Arrays.stream(jars.split(File.pathSeparator)).filter(path -> !path.isBlank()).toList();
    }

    @Test
    void everyDiscoveredOculusBuildSatisfiesTheCompatContract() throws Exception {
        List<byte[]> classes = vroCompatClasses();
        List<String> jars = contractJars();
        assertFalse(jars.isEmpty(), "the build supplies at least the compile-time Oculus jar");
        for (String jar : jars) {
            System.out.println("Oculus contract checked: " + jar);
            assertEquals(List.of(), OculusCompatContract.problems(classes, jar(jar)), jar);
        }
    }

    private static Function<String, byte[]> without(Function<String, byte[]> jar, String owner, String method, String descPrefix) {
        return name -> {
            byte[] bytes = jar.apply(name);
            if (bytes == null || !name.equals(owner)) return bytes;
            var node = new org.objectweb.asm.tree.ClassNode();
            new org.objectweb.asm.ClassReader(bytes).accept(node, 0);
            node.methods.removeIf(m -> m.name.equals(method) && m.desc.startsWith(descPrefix));
            var writer = new org.objectweb.asm.ClassWriter(0);
            node.accept(writer);
            return writer.toByteArray();
        };
    }

    @Test
    void removedInjectionTargetsAndCalledMembersAreReported() throws Exception {
        List<byte[]> classes = vroCompatClasses();
        var stock = jar(contractJars().get(0));
        List<String> noDestroy = OculusCompatContract.problems(classes,
                without(stock, "net/coderbot/iris/pipeline/newshader/NewWorldRenderingPipeline", "destroy", "()V"));
        assertTrue(noDestroy.stream().anyMatch(p -> p.contains("NewWorldRenderingPipeline.destroy")), noDestroy.toString());
        // The 7-argument constructor is both an @Inject target and called by the program compiler.
        List<String> noConstructor = OculusCompatContract.problems(classes,
                without(stock, "net/coderbot/iris/shaderpack/ProgramSource", "<init>",
                        "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lnet/coderbot/iris/shaderpack/ProgramSet;"));
        assertTrue(noConstructor.stream().anyMatch(p -> p.contains("ProgramSource.<init>") && p.contains("mixin")), noConstructor.toString());
        assertTrue(noConstructor.stream().anyMatch(p -> p.contains("ProgramSource.<init>") && p.contains("used by")), noConstructor.toString());
        List<String> noInvokerTarget = OculusCompatContract.problems(classes,
                without(stock, "net/coderbot/iris/pipeline/newshader/NewWorldRenderingPipeline", "createShader",
                        "(Ljava/lang/String;Lnet/coderbot/iris/shaderpack/ProgramSource;"));
        assertTrue(noInvokerTarget.stream().anyMatch(p -> p.contains("createShader")), noInvokerTarget.toString());
    }

    @Test
    void missingOculusClassesAndMembersAreReported() throws Exception {
        List<byte[]> classes = vroCompatClasses();
        var stock = jar(contractJars().get(0));
        assertFalse(OculusCompatContract.problems(classes, name -> null).isEmpty());
        // Drop one class that a VRO mixin injects into.
        List<String> problems = OculusCompatContract.problems(classes,
                name -> name.equals("net/coderbot/iris/shaderpack/ProgramDirectives") ? null : stock.apply(name));
        assertTrue(problems.stream().anyMatch(problem -> problem.contains("ProgramDirectives")), problems.toString());
    }
}
