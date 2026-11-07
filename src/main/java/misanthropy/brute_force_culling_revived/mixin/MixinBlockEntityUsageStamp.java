package misanthropy.brute_force_culling_revived.mixin;

import misanthropy.brute_force_culling_revived.util.UsageStamp;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BlockEntity.class)
public abstract class MixinBlockEntityUsageStamp implements UsageStamp {
    @Unique
    private int bfr$usageStamp = Integer.MIN_VALUE;
    @Unique
    private int bfr$refreshStamp = Integer.MIN_VALUE;

    @Override
    public int bfr$usageStamp() {
        return this.bfr$usageStamp;
    }

    @Override
    public void bfr$usageStamp(int tick) {
        this.bfr$usageStamp = tick;
    }

    @Override
    public int bfr$refreshStamp() {
        return this.bfr$refreshStamp;
    }

    @Override
    public void bfr$refreshStamp(int tick) {
        this.bfr$refreshStamp = tick;
    }
}
