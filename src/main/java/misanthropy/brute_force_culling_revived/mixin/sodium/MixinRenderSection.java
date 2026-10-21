package misanthropy.brute_force_culling_revived.mixin.sodium;

import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import misanthropy.brute_force_culling_revived.api.impl.IRenderSectionVisibility;
import misanthropy.brute_force_culling_revived.api.impl.IVegetationSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(RenderSection.class)
public abstract class MixinRenderSection implements IRenderSectionVisibility, IVegetationSection {

    @Unique
    private volatile boolean bruteForceRenderingRevived$hasVegetation;

    @Unique
    private volatile boolean bruteForceRenderingRevived$vegetationThinned;

    @Override
    public boolean bruteForceRenderingRevived$hasVegetation() {
        return bruteForceRenderingRevived$hasVegetation;
    }

    @Override
    public boolean bruteForceRenderingRevived$isVegetationThinned() {
        return bruteForceRenderingRevived$vegetationThinned;
    }

    @Override
    public void bruteForceRenderingRevived$setVegetation(boolean hasVegetation, boolean thinned) {
        bruteForceRenderingRevived$hasVegetation = hasVegetation;
        bruteForceRenderingRevived$vegetationThinned = thinned;
    }

    @Shadow(remap = false)
    public abstract int getOriginX();

    @Shadow(remap = false)
    public abstract int getOriginY();

    @Shadow(remap = false)
    public abstract int getOriginZ();

    @Unique
    private int bruteForceRenderingRevived$cullingLastVisibleFrame;

    @Override
    public boolean bruteForceRenderingRevived$isVisibleAtFrame(int frame) {
        return frame == bruteForceRenderingRevived$cullingLastVisibleFrame;
    }

    @Override
    public void bruteForceRenderingRevived$markVisibleAtFrame(int frame) {
        bruteForceRenderingRevived$cullingLastVisibleFrame = frame;
    }

    @Override
    public int bruteForceRenderingRevived$getPositionX() {
        return getOriginX();
    }

    @Override
    public int bruteForceRenderingRevived$getPositionY() {
        return getOriginY();
    }

    @Override
    public int bruteForceRenderingRevived$getPositionZ() {
        return getOriginZ();
    }
}
