package dev.hoyin1600p.vault_render_optimization.client.entitygpu;

import java.nio.IntBuffer;
import java.util.Random;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL31C;
import org.lwjgl.opengl.GL42C;
import org.lwjgl.opengl.GL43C;
import org.lwjgl.system.MemoryUtil;

/**
 * The Oculus program's rounding helpers against Java itself: {@code a / b}, {@code (float) Math.sqrt(a)} and
 * {@code NormalHelper.rsqrt(a)} for random bit patterns (every exponent, subnormals, zeros, infinities, NaN) and
 * for the value ranges entity quads produce. NaN results only need to be NaN.
 */
final class IrisMathGl {
    private IrisMathGl() {
    }

    static String run(GpuEntityCapabilities.Route route, long seed, int count) {
        Random random = new Random(seed);
        int[] input = new int[count * 2];
        for (int i = 0; i < count; i++) {
            input[2 * i] = operand(random);
            input[2 * i + 1] = operand(random);
        }
        int program = GpuEntityBackend.compile(route.header() + "#define IRIS_ENTITY 1\n#define IRIS_MATH_TEST 1\n"
                + GpuEntityBackend.loadSource());
        int in = GL15C.glGenBuffers();
        int out = GL15C.glGenBuffers();
        IntBuffer data = MemoryUtil.memAllocInt(input.length);
        IntBuffer result = MemoryUtil.memAllocInt(count * 3);
        try {
            data.put(input).flip();
            GL15C.glBindBuffer(GL43C.GL_SHADER_STORAGE_BUFFER, in);
            GL15C.glBufferData(GL43C.GL_SHADER_STORAGE_BUFFER, data, GL15C.GL_STATIC_DRAW);
            GL15C.glBindBuffer(GL43C.GL_SHADER_STORAGE_BUFFER, out);
            GL15C.glBufferData(GL43C.GL_SHADER_STORAGE_BUFFER, (long) count * 12, GL15C.GL_STATIC_DRAW);
            GL20C.glUseProgram(program);
            bind(program, "Instances", 1, in);
            bind(program, "Vertices", 2, out);
            GL30C.glUniform1ui(GL20C.glGetUniformLocation(program, "instanceCount"), count);
            GL43C.glDispatchCompute((count + 63) / 64, 1, 1);
            GL42C.glMemoryBarrier(GL42C.GL_BUFFER_UPDATE_BARRIER_BIT);
            GL15C.glBindBuffer(GL43C.GL_SHADER_STORAGE_BUFFER, out);
            GL15C.glGetBufferSubData(GL43C.GL_SHADER_STORAGE_BUFFER, 0L, result);
            GL15C.glBindBuffer(GL43C.GL_SHADER_STORAGE_BUFFER, 0);
            GL20C.glUseProgram(0);
            for (int i = 0; i < count; i++) {
                float a = Float.intBitsToFloat(input[2 * i]), b = Float.intBitsToFloat(input[2 * i + 1]);
                String failure = check("divide", a + " / " + b, a / b, result.get(3 * i));
                if (failure == null) failure = check("sqrt", String.valueOf(a), (float) Math.sqrt(a), result.get(3 * i + 1));
                if (failure == null) failure = check("rsqrt", String.valueOf(a), IrisEntityExtension.rsqrt(a), result.get(3 * i + 2));
                if (failure != null) return failure;
            }
            return null;
        } finally {
            GL20C.glDeleteProgram(program);
            GL15C.glDeleteBuffers(in);
            GL15C.glDeleteBuffers(out);
            MemoryUtil.memFree(data);
            MemoryUtil.memFree(result);
        }
    }

    private static void bind(int program, String block, int binding, int buffer) {
        int index = GL43C.glGetProgramResourceIndex(program, GL43C.GL_SHADER_STORAGE_BLOCK, block);
        if (index == GL31C.GL_INVALID_INDEX) throw new IllegalStateException(block + " missing");
        GL43C.glShaderStorageBlockBinding(program, index, binding);
        GL30C.glBindBufferBase(GL43C.GL_SHADER_STORAGE_BUFFER, binding, buffer);
    }

    private static String check(String op, String operands, float expected, int gpuBits) {
        float gpu = Float.intBitsToFloat(gpuBits);
        if (Float.isNaN(expected) ? Float.isNaN(gpu) : Float.floatToRawIntBits(expected) == gpuBits) return null;
        return op + "(" + operands + "): Java " + expected + " (0x" + Integer.toHexString(Float.floatToRawIntBits(expected))
                + ") GPU " + gpu + " (0x" + Integer.toHexString(gpuBits) + ")";
    }

    private static int operand(Random random) {
        return switch (random.nextInt(8)) {
            case 0 -> random.nextInt();                                          // any bit pattern
            case 1 -> random.nextInt(0x00800000) | (random.nextBoolean() ? 0x80000000 : 0); // subnormals and zeros
            case 2 -> new int[]{0, 0x80000000, 0x7F800000, 0xFF800000, 0x7FC00000, 0x00000001, 0x7F7FFFFF,
                    0x3F800000}[random.nextInt(8)];
            case 3 -> Float.floatToRawIntBits((random.nextFloat() - 0.5F) * 2);        // normal components
            case 4 -> Float.floatToRawIntBits(random.nextFloat() * random.nextFloat() * 1e-3F); // squared lengths
            default -> Float.floatToRawIntBits((random.nextFloat() - 0.5F) * (float) Math.pow(2, random.nextInt(60) - 30));
        };
    }
}
