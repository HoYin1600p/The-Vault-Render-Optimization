package dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.compiler;

import com.jozufozu.flywheel.backend.gl.shader.GlProgram;
import com.jozufozu.flywheel.core.compile.ProgramContext;
import com.jozufozu.flywheel.core.compile.Template;
import com.jozufozu.flywheel.core.compile.VertexData;
import com.jozufozu.flywheel.core.shader.WorldProgram;
import com.jozufozu.flywheel.core.source.FileResolution;
import net.coderbot.iris.Iris;
import net.coderbot.iris.gl.blending.AlphaTest;
import net.coderbot.iris.gl.blending.AlphaTestFunction;
import net.coderbot.iris.gl.blending.BlendModeOverride;
import net.coderbot.iris.gl.shader.StandardMacros;
import net.coderbot.iris.pipeline.WorldRenderingPipeline;
import net.coderbot.iris.pipeline.newshader.NewWorldRenderingPipeline;
import net.coderbot.iris.shaderpack.*;
import net.coderbot.iris.shaderpack.loading.ProgramId;
import net.coderbot.iris.shaderpack.preprocessor.JcppProcessor;
import net.minecraft.resources.ResourceLocation;
import dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.FlywheelShaderCompatState;
import dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.VroFlywheelShaderCompat;
import dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.accessors.IrisRenderingPipelineAccessor;
import dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.accessors.ProgramDirectivesAccessor;
import dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.accessors.ProgramSetAccessor;
import dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.accessors.ProgramSourceAccessor;
import dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.transformer.ShaderPatcherBase;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class NewProgramCompiler <TP extends ShaderPatcherBase,P extends WorldProgram> extends IrisProgramCompilerBase<P>{
    private final Map<ProgramSet,ProgramFallbackResolver> resolvers = new HashMap<>();
    private final Set<ProgramSet> disabledDedicatedGbuffers = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<ProgramSet> disabledDedicatedShadow = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Iterable<StringPair> environmentDefines;
    public NewProgramCompiler(GlProgram.Factory<P> factory, Template<? extends VertexData> template, FileResolution header,Class<TP> patcherClass) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        super(factory, template, header);
        //environmentDefines = StandardMacros.createStandardEnvironmentDefines();
        Method method = StandardMacros.class.getMethod("createStandardEnvironmentDefines");
        environmentDefines =(Iterable<StringPair>) method.invoke(null);
        patcher = patcherClass.getDeclaredConstructor(Template.class, FileResolution.class).newInstance(template,header);
    }

    private final TP patcher;

    @Override
    P createIrisShaderProgram(ProgramContext ctx, boolean isShadow) {
        WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();
        if (pipeline instanceof NewWorldRenderingPipeline newPipeline) {
            ProgramSet programSet = ((IrisRenderingPipelineAccessor) newPipeline).getProgramSet();
            Set<ProgramSet> disabledDedicated = isShadow ? disabledDedicatedShadow : disabledDedicatedGbuffers;
            Optional<ProgramSource> dedicatedSource = getDedicatedProgramSource(programSet, isShadow);
            boolean dedicatedFailed = false;

            if (dedicatedSource.isPresent() && !disabledDedicated.contains(programSet)) {
                P dedicatedProgram = compileProgram(ctx, isShadow, newPipeline, programSet, dedicatedSource.get());
                if (dedicatedProgram != null) {
                    FlywheelShaderCompatState.recordProgramSource(isShadow, true, false);
                    return dedicatedProgram;
                }

                dedicatedFailed = true;
                disabledDedicated.add(programSet);
                VroFlywheelShaderCompat.LOGGER.warn(
                        "Dedicated {} program failed; retrying with generated compatibility shader",
                        isShadow ? "shadow_flw" : "gbuffers_flw"
                );
            }

            Optional<ProgramSource> sourceReferenceOpt = getGeneratedProgramSource(programSet, ctx.spec.name, isShadow);
            if(sourceReferenceOpt.isEmpty())
                return null;

            P generatedProgram = compileProgram(ctx, isShadow, newPipeline, programSet, sourceReferenceOpt.get());
            if (generatedProgram != null) {
                FlywheelShaderCompatState.recordProgramSource(isShadow, false, dedicatedFailed);
            }
            return generatedProgram;
        }
        return null;
    }

    private P compileProgram(ProgramContext ctx, boolean isShadow, NewWorldRenderingPipeline pipeline,
                             ProgramSet programSet, ProgramSource source) {
        if (source.getVertexSource().isEmpty()) {
            return null;
        }

        try {
            String newVertexSource = patcher.patch(source.getVertexSource().get(),
                    new ShaderPatcherBase.Context(ctx.spec.getVertexFile(), ctx.ctx, ctx.vertexType));
            newVertexSource = JcppProcessor.glslPreprocessSource(newVertexSource, environmentDefines);
            ProgramSource newProgramSource = programSourceOverrideVertexSource(ctx, programSet, source, newVertexSource);
            ((ProgramDirectivesAccessor) newProgramSource.getDirectives()).setFlwAlphaTestOverride(
                    new AlphaTest(AlphaTestFunction.GREATER, ctx.alphaDiscard));
            return createWorldProgramBySource(ctx, isShadow,
                    (IrisRenderingPipelineAccessor) pipeline, newProgramSource);
        } catch (RuntimeException exception) {
            VroFlywheelShaderCompat.LOGGER.warn(
                    "Could not transform Flywheel shader candidate {}",
                    source.getName(),
                    exception
            );
            return null;
        }
    }

    private Optional<ProgramSource> getDedicatedProgramSource(ProgramSet programSet, boolean isShadow) {
        ProgramSetAccessor accessor = (ProgramSetAccessor) programSet;
        return isShadow ? accessor.getShadowFlw() : accessor.getGbuffersFlw();
    }

    protected Optional<ProgramSource> getGeneratedProgramSource(ProgramSet programSet, ResourceLocation flwShaderName, boolean isShadow){

        // Tessellation is currently not supported
        var resolver = resolvers.computeIfAbsent(programSet, ProgramFallbackResolver::new);

        if(isShadow){
            var shadow = resolver.resolve(ProgramId.Shadow).orElse(null);
            if(shadow==null)
                return Optional.empty();
            ShaderProperties properties = ((ProgramSourceAccessor) shadow).getShaderProperties();
            BlendModeOverride blendModeOverride = ((ProgramSourceAccessor) shadow).getBlendModeOverride();
            return Optional.of(new ProgramSource("shadow_flw",
                    shadow.getVertexSource().orElseThrow(),
                    shadow.getGeometrySource().orElse(null),
                    shadow.getFragmentSource().orElseThrow(),
                    programSet, properties, blendModeOverride));
        }else{
            var refProgramId = ProgramId.Block;
            if(Objects.equals(flwShaderName.getNamespace(), "flywheel")
                && Objects.equals(flwShaderName.getPath(), "passthru")){
                // Temporarily hardcoded, maybe configurable in the future
                refProgramId = ProgramId.Terrain;
            }
            var refProgram = resolver.resolve(refProgramId).orElse(null);
            if(refProgram==null)
                return Optional.empty();

            ShaderProperties properties = ((ProgramSourceAccessor) refProgram).getShaderProperties();
            BlendModeOverride blendModeOverride = ((ProgramSourceAccessor) refProgram).getBlendModeOverride();

            return Optional.of(new ProgramSource("gbuffer_flw",
                    refProgram.getVertexSource().orElseThrow(),
                    refProgram.getGeometrySource().orElse(null),
                    refProgram.getFragmentSource().orElseThrow(),
                    programSet, properties, blendModeOverride));
        }
    }

    @Override
    public void clear() {
        super.clear();
        resolvers.clear();
        disabledDedicatedGbuffers.clear();
        disabledDedicatedShadow.clear();
    }
}
