package misanthropy.brute_force_culling_revived.mixin;

import misanthropy.brute_force_culling_revived.api.Config;
import misanthropy.brute_force_culling_revived.api.CullingStateManager;
import misanthropy.brute_force_culling_revived.util.DummySection;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Frustum.class)
public abstract class MixinFrustum {

    @Unique
    private static final ThreadLocal<DummySection> DUMMY_SECTION = ThreadLocal.withInitial(DummySection::new);

    @ModifyReturnValue(method = "isVisible", at = @At(value = "RETURN"))
    public boolean afterVisible(boolean visible, @NotNull AABB aabb) {
        if (!visible || !CullingStateManager.applyFrustum || !Config.shouldCullChunk()) {
            return visible;
        }
        double sizeX = aabb.maxX - aabb.minX;
        if (sizeX < 15.0 || sizeX > 17.0) {
            return true;
        }

        DummySection section = DUMMY_SECTION.get();

        section.set(
                (int) ((aabb.minX + aabb.maxX) * 0.5D),
                (int) ((aabb.minY + aabb.maxY) * 0.5D),
                (int) ((aabb.minZ + aabb.maxZ) * 0.5D)
        );

        boolean culled = !CullingStateManager.shouldRenderChunk(section, true);
        CullingStateManager.countChunk(culled);
        return !culled;
    }
}
