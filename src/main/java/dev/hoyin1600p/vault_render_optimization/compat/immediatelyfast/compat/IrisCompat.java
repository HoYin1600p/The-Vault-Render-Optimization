/*
 * This file is part of ImmediatelyFast Reforged - https://github.com/CCr4ft3r/ImmediatelyFastReforged
 * Copyright (C) 2023 RK_01/RaphiMC and contributors
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Modified by HoYin1600p for The Vault Render Optimization (VRO), 2026: relocated from
 * net.raphimc.immediatelyfast; the Reflect library accessors are replaced with MethodHandles on
 * Oculus's public static fields; a byte-level preflight runs before mixins apply; a failure
 * disables the Oculus path instead of exiting the game.
 */
package dev.hoyin1600p.vault_render_optimization.compat.immediatelyfast.compat;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.hoyin1600p.vault_render_optimization.compat.immediatelyfast.VroImmediatelyFast;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import net.minecraftforge.fml.loading.LoadingModList;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

public class IrisCompat {
    private static final String IMMEDIATE_STATE = "net/coderbot/iris/vertices/ImmediateState";
    private static final String EXTENDING_BUILDER = "net/coderbot/iris/vertices/ExtendingBufferBuilder";
    private static final String BEGIN_DESC = "(Lcom/mojang/blaze3d/vertex/VertexFormat$Mode;Lcom/mojang/blaze3d/vertex/VertexFormat;)V";

    public static boolean IRIS_LOADED = false;

    public static BooleanSupplier isRenderingLevel;
    public static BooleanConsumer renderWithExtendedVertexFormat;
    public static TriConsumer<BufferBuilder, VertexFormat.Mode, VertexFormat> iris$beginWithoutExtending;

    /** Null when the installed Oculus has the members this compatibility uses; checked from bytes. */
    public static String preflight(LoadingModList mods) {
        var oculus = mods.getModFileById("oculus");
        if (oculus == null) return "Oculus could not be located";
        return preflight(name -> {
            try {
                Path path = oculus.getFile().findResource(name + ".class");
                return Files.exists(path) ? Files.readAllBytes(path) : null;
            } catch (IOException failure) {
                throw new UncheckedIOException(failure);
            }
        });
    }

    /** Same check over raw class bytes by internal name (null when absent). */
    public static String preflight(Function<String, byte[]> classes) {
        try {
            ClassNode state = read(classes, IMMEDIATE_STATE);
            ClassNode builder = read(classes, EXTENDING_BUILDER);
            if (state == null || builder == null) return "its ImmediateState/ExtendingBufferBuilder classes are missing";
            for (String name : new String[]{"isRenderingLevel", "renderWithExtendedVertexFormat"}) {
                boolean found = state.fields.stream().anyMatch(f -> f.name.equals(name) && f.desc.equals("Z")
                        && (f.access & Opcodes.ACC_STATIC) != 0 && (f.access & Opcodes.ACC_PUBLIC) != 0);
                if (!found) return "ImmediateState." + name + " is missing";
            }
            boolean begin = builder.methods.stream().anyMatch(m -> m.name.equals("iris$beginWithoutExtending")
                    && m.desc.equals(BEGIN_DESC));
            return begin ? null : "ExtendingBufferBuilder.iris$beginWithoutExtending is missing";
        } catch (RuntimeException failure) {
            return "its classes could not be read (" + failure + ")";
        }
    }

    private static ClassNode read(Function<String, byte[]> classes, String internalName) {
        byte[] bytes = classes.apply(internalName);
        if (bytes == null) return null;
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, ClassReader.SKIP_CODE);
        return node;
    }

    /** Called from VRO's mod constructor when the built-in copy is active and Oculus is installed. */
    public static void init() {
        try {
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();
            Class<?> immediateState = Class.forName("net.coderbot.iris.vertices.ImmediateState");
            Class<?> extendingBuilder = Class.forName("net.coderbot.iris.vertices.ExtendingBufferBuilder");
            MethodHandle getRenderingLevel = lookup.findStaticGetter(immediateState, "isRenderingLevel", boolean.class);
            MethodHandle setExtended = lookup.findStaticSetter(immediateState, "renderWithExtendedVertexFormat", boolean.class);
            MethodHandle begin = lookup.findVirtual(extendingBuilder, "iris$beginWithoutExtending",
                    MethodType.methodType(void.class, VertexFormat.Mode.class, VertexFormat.class));
            isRenderingLevel = () -> {
                try {
                    return (boolean) getRenderingLevel.invokeExact();
                } catch (Throwable failure) {
                    throw new IllegalStateException(failure);
                }
            };
            renderWithExtendedVertexFormat = value -> {
                try {
                    setExtended.invokeExact(value);
                } catch (Throwable failure) {
                    throw new IllegalStateException(failure);
                }
            };
            iris$beginWithoutExtending = (bufferBuilder, mode, format) -> {
                try {
                    begin.invoke(bufferBuilder, mode, format);
                } catch (Throwable failure) {
                    throw new IllegalStateException(failure);
                }
            };
            IRIS_LOADED = true;
        } catch (Throwable failure) {
            // The preflight already checked these members; if the lookup still fails, keep the
            // non-Oculus path rather than exiting the game as the original did.
            IRIS_LOADED = false;
            VroImmediatelyFast.LOGGER.error("Built-in ImmediatelyFast could not bind Oculus compatibility", failure);
        }
    }
}
