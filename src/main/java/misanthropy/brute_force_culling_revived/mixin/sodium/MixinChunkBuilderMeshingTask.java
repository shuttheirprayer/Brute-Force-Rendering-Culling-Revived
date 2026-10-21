package misanthropy.brute_force_culling_revived.mixin.sodium;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildContext;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildOutput;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderContext;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderMeshingTask;
import me.jellysquid.mods.sodium.client.util.task.CancellationToken;
import misanthropy.brute_force_culling_revived.api.impl.IVegetationSection;
import misanthropy.brute_force_culling_revived.util.VegetationThinning;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkBuilderMeshingTask.class)
public abstract class MixinChunkBuilderMeshingTask {
    @Shadow(remap = false)
    @Final
    private RenderSection render;

    @Unique
    private boolean bruteForceRenderingRevived$thin;

    @Unique
    private boolean bruteForceRenderingRevived$sawVegetation;

    @Inject(method = "execute(Lme/jellysquid/mods/sodium/client/render/chunk/compile/ChunkBuildContext;Lme/jellysquid/mods/sodium/client/util/task/CancellationToken;)Lme/jellysquid/mods/sodium/client/render/chunk/compile/ChunkBuildOutput;", at = @At("HEAD"), remap = false)
    private void bruteForceRenderingRevived$onExecuteHead(ChunkBuildContext context, CancellationToken token, CallbackInfoReturnable<ChunkBuildOutput> cir) {
        bruteForceRenderingRevived$thin = VegetationThinning.isFar(render.getChunkX(), render.getChunkZ());
        bruteForceRenderingRevived$sawVegetation = false;
    }

    @WrapOperation(
            method = "execute(Lme/jellysquid/mods/sodium/client/render/chunk/compile/ChunkBuildContext;Lme/jellysquid/mods/sodium/client/util/task/CancellationToken;)Lme/jellysquid/mods/sodium/client/render/chunk/compile/ChunkBuildOutput;",
            at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/render/chunk/compile/pipeline/BlockRenderer;renderModel(Lme/jellysquid/mods/sodium/client/render/chunk/compile/pipeline/BlockRenderContext;Lme/jellysquid/mods/sodium/client/render/chunk/compile/ChunkBuildBuffers;)V"),
            remap = false
    )
    private void bruteForceRenderingRevived$thinVegetation(BlockRenderer renderer, BlockRenderContext ctx, ChunkBuildBuffers buffers, Operation<Void> original) {
        if ((bruteForceRenderingRevived$thin || !bruteForceRenderingRevived$sawVegetation) && VegetationThinning.isVegetation(ctx.state())) {
            bruteForceRenderingRevived$sawVegetation = true;
            if (bruteForceRenderingRevived$thin && VegetationThinning.isHidden(ctx.state(), ctx.pos())) return;
        }
        original.call(renderer, ctx, buffers);
    }

    @Inject(method = "execute(Lme/jellysquid/mods/sodium/client/render/chunk/compile/ChunkBuildContext;Lme/jellysquid/mods/sodium/client/util/task/CancellationToken;)Lme/jellysquid/mods/sodium/client/render/chunk/compile/ChunkBuildOutput;", at = @At("RETURN"), remap = false)
    private void bruteForceRenderingRevived$onExecuteReturn(ChunkBuildContext context, CancellationToken token, CallbackInfoReturnable<ChunkBuildOutput> cir) {
        if (cir.getReturnValue() != null) {
            ((IVegetationSection) render).bruteForceRenderingRevived$setVegetation(bruteForceRenderingRevived$sawVegetation, bruteForceRenderingRevived$thin);
        }
    }
}
