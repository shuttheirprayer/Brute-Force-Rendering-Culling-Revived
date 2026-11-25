package misanthropy.brute_force_culling_revived.mixin.sodium;

import me.jellysquid.mods.sodium.client.render.viewport.Viewport;
import me.jellysquid.mods.sodium.client.render.viewport.frustum.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = Viewport.class, remap = false)
public interface AccessorViewport {
    @Accessor("frustum")
    Frustum bruteForceRenderingRevived$frustum();
}
