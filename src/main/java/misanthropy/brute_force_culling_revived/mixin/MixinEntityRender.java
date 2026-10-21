package misanthropy.brute_force_culling_revived.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import misanthropy.brute_force_culling_revived.api.CullingStateManager;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntityRenderDispatcher.class)
public class MixinEntityRender {
    @ModifyReturnValue(method = "shouldRender", at = @At("RETURN"))
    private boolean onShouldRender(boolean original, @Local(argsOnly = true) Entity p_114398_) {
        if (original && !p_114398_.noCulling && CullingStateManager.shouldSkipEntity(p_114398_)) {
            return false;
        }
        return original;
    }
}
