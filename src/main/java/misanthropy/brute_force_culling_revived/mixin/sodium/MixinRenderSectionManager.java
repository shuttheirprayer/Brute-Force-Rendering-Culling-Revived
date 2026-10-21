package misanthropy.brute_force_culling_revived.mixin.sodium;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkUpdateType;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager;
import me.jellysquid.mods.sodium.client.render.chunk.lists.SortedRenderLists;
import me.jellysquid.mods.sodium.client.render.chunk.lists.VisibleChunkCollector;
import me.jellysquid.mods.sodium.client.render.viewport.Viewport;
import misanthropy.brute_force_culling_revived.api.Config;
import misanthropy.brute_force_culling_revived.api.CullingStateManager;
import misanthropy.brute_force_culling_revived.api.impl.IRenderSectionVisibility;
import misanthropy.brute_force_culling_revived.api.impl.IVegetationSection;
import misanthropy.brute_force_culling_revived.mixin.AccessorFrustum;
import misanthropy.brute_force_culling_revived.util.SodiumSectionAsyncUtil;
import misanthropy.brute_force_culling_revived.util.VegetationThinning;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderSectionManager.class)
public abstract class MixinRenderSectionManager {

    @Shadow(remap = false)
    @Final
    private Long2ReferenceMap<RenderSection> sectionByPosition;

    @Shadow(remap = false)
    private @NotNull SortedRenderLists renderLists;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void init(ClientLevel world, int renderDistance, CommandList commandList, CallbackInfo ci) {
        SodiumSectionAsyncUtil.reset();
        SodiumSectionAsyncUtil.fromSectionManager(this.sectionByPosition, world);
    }

    @Inject(method = "destroy", at = @At("HEAD"), remap = false)
    private void onDestroy(CallbackInfo ci) {
        SodiumSectionAsyncUtil.reset();
    }

    @ModifyReturnValue(method = "isSectionVisible", at = @At(value = "RETURN"), remap = false)
    private boolean onIsSectionVisible(boolean visible, int x, int y, int z, @Local RenderSection section) {
        if (!Config.shouldCullChunk()) return visible;

        if (!CullingStateManager.shouldRenderChunk((IRenderSectionVisibility) section, false)) {
            return false;
        }

        int ox = section.getOriginX();
        int oy = section.getOriginY();
        int oz = section.getOriginZ();
        return ((AccessorFrustum) CullingStateManager.FRUSTUM).invokeCubeInFrustum(ox, oy, oz, ox + 16, oy + 16, oz + 16);
    }

    @Inject(method = "update", at = @At(value = "HEAD"), remap = false, cancellable = true)
    private void onUpdate(Camera camera, Viewport viewport, int frame, boolean spectator, @NotNull CallbackInfo ci) {
        BlockPos cameraPos = camera.getBlockPosition();
        if (VegetationThinning.update(cameraPos.getX() >> 4, cameraPos.getZ() >> 4, Minecraft.getInstance().options.getEffectiveRenderDistance())) {
            for (RenderSection section : this.sectionByPosition.values()) {
                IVegetationSection vegetation = (IVegetationSection) section;
                if (!vegetation.bruteForceRenderingRevived$hasVegetation() || !section.isBuilt()) continue;
                if (vegetation.bruteForceRenderingRevived$isVegetationThinned() == VegetationThinning.isFar(section.getChunkX(), section.getChunkZ())) continue;
                if (ChunkUpdateType.canPromote(section.getPendingUpdate(), ChunkUpdateType.REBUILD)) {
                    section.setPendingUpdate(ChunkUpdateType.REBUILD);
                }
            }
        }
        if (CullingStateManager.checkCulling && CullingStateManager.DEBUG > 1) {
            ci.cancel();
        }
        CullingStateManager.updating();
    }

    @ModifyVariable(
            name = "visitor",
            method = "createTerrainRenderList",
            at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/render/chunk/occlusion/OcclusionCuller;findVisible(Lme/jellysquid/mods/sodium/client/render/chunk/occlusion/OcclusionCuller$Visitor;Lme/jellysquid/mods/sodium/client/render/viewport/Viewport;FZI)V"),
            remap = false
    )
    private VisibleChunkCollector onCreateTerrainRenderList(VisibleChunkCollector visitor) {
        if (!Config.getAsyncChunkRebuild()) return visitor;
        VisibleChunkCollector collector = SodiumSectionAsyncUtil.getActiveCollector();
        return collector == null ? visitor : collector;
    }

    @Inject(method = "updateChunks", at = @At(value = "HEAD"), remap = false)
    private void onCreateTerrainRenderList(boolean updateImmediately, CallbackInfo ci) {
        if (!Config.getAsyncChunkRebuild()) return;
        VisibleChunkCollector collector = SodiumSectionAsyncUtil.getActiveCollector();
        if (collector != null) this.renderLists = collector.createRenderLists();
    }
}