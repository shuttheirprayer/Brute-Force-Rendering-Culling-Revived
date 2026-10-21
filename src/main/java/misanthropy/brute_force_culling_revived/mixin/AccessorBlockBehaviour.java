package misanthropy.brute_force_culling_revived.mixin;

import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlockBehaviour.class)
public interface AccessorBlockBehaviour {
    @Accessor("hasCollision")
    boolean bruteForceRenderingRevived$hasCollision();
}
