package dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.compiler;

import com.jozufozu.flywheel.backend.gl.shader.GlProgram;
import com.jozufozu.flywheel.core.compile.ProgramContext;
import com.jozufozu.flywheel.core.compile.Template;
import com.jozufozu.flywheel.core.compile.VertexData;
import com.jozufozu.flywheel.core.shader.WorldProgram;
import com.jozufozu.flywheel.core.source.FileResolution;
import net.coderbot.iris.Iris;
import net.coderbot.iris.gl.blending.AlphaTest;
import net.coderbot.iris.gl.blending.BlendModeOverride;
import net.coderbot.iris.pipeline.WorldRenderingPipeline;
import net.coderbot.iris.pipeline.newshader.FogMode;
import net.coderbot.iris.shaderpack.ProgramSet;
import net.coderbot.iris.shaderpack.ProgramSource;
import net.coderbot.iris.shaderpack.ShaderProperties;
import net.coderbot.iris.shaderpack.loading.ProgramId;
import net.coderbot.iris.vertices.IrisVertexFormats;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.VroFlywheelShaderCompat;
import dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.FlywheelShaderCompatState;
import dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.accessors.IrisRenderingPipelineAccessor;
import dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.accessors.ProgramSourceAccessor;
import dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.accessors.WorldProgramAccessor;
import dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.flywheel.IrisFlwCompatShaderWarp;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public abstract class IrisProgramCompilerBase<P extends WorldProgram> {
    // Every live compiler, so a destroyed Oculus pipeline can be dropped from all of their caches.
    private static final Set<IrisProgramCompilerBase<?>> COMPILERS = Collections.newSetFromMap(new WeakHashMap<>());

    Map<WorldRenderingPipeline, HashMap<ProgramContext, P>> programCache = new HashMap<>();

    Map<WorldRenderingPipeline, HashMap<ProgramContext, P>> shadowProgramCache = new HashMap<>();

    protected final GlProgram.Factory<P> factory;
    private static int programCounter = 0;

    public IrisProgramCompilerBase(GlProgram.Factory<P> factory, Template<? extends VertexData> ignoredTemplate, FileResolution ignoredHeader) {
        this.factory = factory;
        synchronized (COMPILERS) {
            COMPILERS.add(this);
        }
    }

    /**
     * Oculus closes every shader it created for the pipeline (including ours, which it tracks in
     * {@code loadedShaders}) when the pipeline is destroyed. Only the stale references are dropped
     * here; the programs must not be deleted a second time.
     */
    public static void forgetPipeline(WorldRenderingPipeline pipeline) {
        synchronized (COMPILERS) {
            for (IrisProgramCompilerBase<?> compiler : COMPILERS) {
                compiler.forget(pipeline);
            }
        }
    }

    /** Distinct Oculus pipelines still referenced by any compiler cache; at most 1 after pack switches. */
    public static String cachedPipelines() {
        Set<WorldRenderingPipeline> pipelines = Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        int compilers;
        synchronized (COMPILERS) {
            compilers = COMPILERS.size();
            for (IrisProgramCompilerBase<?> compiler : COMPILERS) {
                pipelines.addAll(compiler.programCache.keySet());
                pipelines.addAll(compiler.shadowProgramCache.keySet());
            }
        }
        return "program caches: " + compilers + " compilers, " + pipelines.size() + " cached pipelines";
    }

    protected void forget(WorldRenderingPipeline pipeline) {
        programCache.remove(pipeline);
        shadowProgramCache.remove(pipeline);
    }

    public P getProgram(ProgramContext ctx, boolean isShadow) {

        if (FlywheelShaderCompatState.isRenderPathActive()) {
            WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();
            HashMap<ProgramContext, P> cache;
            if (isShadow) {
                cache = shadowProgramCache.computeIfAbsent(pipeline, key -> new HashMap<>());
            } else {
                cache = programCache.computeIfAbsent(pipeline, key -> new HashMap<>());
            }
            if (!cache.containsKey(ctx)) {
                P created = createIrisShaderProgram(ctx, isShadow);
                cache.put(ctx, created);
                if (created == null) {
                    FlywheelShaderCompatState.recordFailure(ctx.spec.name.toString(), null);
                } else {
                    FlywheelShaderCompatState.recordSuccess();
                }
            }
            return cache.get(ctx);
        }
        return null;
    }

    private String getFlwShaderName(ResourceLocation location, boolean isShadow) {
        String randomId = String.valueOf(programCounter);
        programCounter++;
        if (isShadow)
            return String.format("shadow_flw_%s_%s_%s", location.getNamespace(),
                    location.getPath(), randomId);
        else
            return String.format("gbuffers_flw_%s_%s_%s", location.getNamespace(),
                    location.getPath(), randomId);
    }

    abstract P createIrisShaderProgram(ProgramContext ctx, boolean isShadow);

    protected P createWorldProgramBySource(ProgramContext ctx, boolean isShadow, IrisRenderingPipelineAccessor pipeline, ProgramSource processedSource) {
        ShaderInstance override = null;
        try {
            if (isShadow) {
                override = pipeline.callCreateShadowShader(
                        getFlwShaderName(ctx.spec.name, true), processedSource, ProgramId.Block, AlphaTest.ALWAYS,
                        IrisVertexFormats.TERRAIN, false, false, false);
            } else {
                override = pipeline.callCreateShader(
                        getFlwShaderName(ctx.spec.name, false), processedSource, ProgramId.Block, AlphaTest.ALWAYS,
                        IrisVertexFormats.TERRAIN, FogMode.OFF, false, false, false, false);
            }

        } catch (Exception exception) {
            VroFlywheelShaderCompat.LOGGER.warn("Could not compile Flywheel shader candidate", exception);
        }

        if (override != null) {
            P program = factory.create(ctx.spec.name, override.getId());
            ((WorldProgramAccessor) program).setShader(new IrisFlwCompatShaderWarp(override));
            return program;
        }
        return null;
    }

    @NotNull
    protected ProgramSource programSourceOverrideVertexSource(ProgramContext ctx, ProgramSet programSet, ProgramSource source, String vertexSource) {
        ShaderProperties properties = ((ProgramSourceAccessor) source).getShaderProperties();
        BlendModeOverride blendModeOverride = ((ProgramSourceAccessor) source).getBlendModeOverride();
        //Get a copy of program
        return new ProgramSource(source.getName() + "_" + ctx.spec.name.getNamespace() + "_" +
                ctx.spec.name.getPath(), vertexSource,
                source.getGeometrySource().orElse(null),
                source.getFragmentSource().orElse(null), programSet, properties, blendModeOverride);
    }

    public void clear() {
        programCache.clear();
        shadowProgramCache.clear();
    }
}
