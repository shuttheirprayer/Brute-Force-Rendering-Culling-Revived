package misanthropy.brute_force_culling_revived.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface AccessorLivingEntity {
    @Accessor("lerpSteps")
    int getLerpSteps();

    @Accessor("lerpSteps")
    void setLerpSteps(int steps);

    @Accessor("lerpX")
    double getLerpX();

    @Accessor("lerpY")
    double getLerpY();

    @Accessor("lerpZ")
    double getLerpZ();
}
