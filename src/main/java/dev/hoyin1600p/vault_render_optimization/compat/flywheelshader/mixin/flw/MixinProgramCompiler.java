package dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.mixin.flw;

import com.jozufozu.flywheel.backend.gl.shader.GlProgram;
import com.jozufozu.flywheel.core.compile.*;
import com.jozufozu.flywheel.core.shader.WorldProgram;
import net.coderbot.iris.Iris;
import net.coderbot.iris.pipeline.WorldRenderingPipeline;
import net.coderbot.iris.pipeline.newshader.ExtendedShader;
import net.coderbot.iris.pipeline.newshader.NewWorldRenderingPipeline;
import net.coderbot.iris.pipeline.newshader.ShaderKey;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.client.renderer.ShaderInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.VroFlywheelShaderCompat;
import dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.FlywheelShaderCompatState;
import dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.compiler.IrisProgramCompilerBase;
import dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.compiler.NewProgramCompiler;
import dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.mixin.flw.AccessorVertexCompiler;
import dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.mixin.iris.AccessorExtendedShader;
import dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.transformer.GlslTransformerShaderPatcher;

import java.lang.reflect.InvocationTargetException;

@Mixin(value = ProgramCompiler.class, remap = false)
public abstract class MixinProgramCompiler<P extends WorldProgram> {

    private IrisProgramCompilerBase<P> irisProgramCompiler;

    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    public void injectInit(GlProgram.Factory<P> factory, VertexCompiler vertexCompiler, FragmentCompiler fragmentCompiler, CallbackInfo ci) {
        AccessorVertexCompiler vertexCompilerAccessor = (AccessorVertexCompiler) vertexCompiler;
        Template<? extends VertexData> template = vertexCompilerAccessor.getTemplate();
        try {
            irisProgramCompiler = new NewProgramCompiler<>(factory, template, vertexCompilerAccessor.getHeader(), GlslTransformerShaderPatcher.class);
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            VroFlywheelShaderCompat.LOGGER.error("Fail to create program compiler",e);
            FlywheelShaderCompatState.recordFailure("compiler initialization", e);
        }
    }

    @Inject(method = "getProgram", at = @At("HEAD"), remap = false, cancellable = true)
    public void getProgram(ProgramContext ctx, CallbackInfoReturnable<P> cir) {

        try{
            if (FlywheelShaderCompatState.isRenderPathActive()) {
                if (irisProgramCompiler == null) {
                    FlywheelShaderCompatState.recordFailure("compiler initialization", null);
                    return;
                }
                //Optional<ShaderPack> currentPackOptional = Iris.getCurrentPack();
                WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();
                boolean isShadow = IrisApi.getInstance().isRenderingShadowPass();

                P program = irisProgramCompiler.getProgram(ctx,isShadow);
                if (program != null) cir.setReturnValue(program);
                else {
                    if (pipeline instanceof NewWorldRenderingPipeline newPipeline) {
                        ShaderInstance shader = newPipeline.getShaderMap().getShader(ShaderKey.TEXTURED_COLOR);
                        if (shader instanceof ExtendedShader extendedShader) {
                            ((AccessorExtendedShader) extendedShader).getWritingToBeforeTranslucent().bind();
                            //Use the same render target with Gbuffers_textured.
                        }
                    }
                }
            }
        }catch (Exception e){
            VroFlywheelShaderCompat.LOGGER.error("Fail to create iris shader with "+ctx.toString(),e);
            FlywheelShaderCompatState.recordFailure(ctx.spec.name.toString(), e);
        }

    }

    @Inject(method = "invalidate", remap = false, at = @At("TAIL"))
    private void injectInvalidate(CallbackInfo ci) {
        if (irisProgramCompiler != null) {
            irisProgramCompiler.clear();
        }
    }
}
