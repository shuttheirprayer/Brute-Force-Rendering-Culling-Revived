package misanthropy.brute_force_culling_revived.mixin;

import net.minecraft.client.renderer.culling.Frustum;
import org.joml.FrustumIntersection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@SuppressWarnings("unused")
@Mixin(Frustum.class)
public interface AccessorFrustum {
    @Accessor("camX")
    double camX();

    @Accessor("camY")
    double camY();

    @Accessor("camZ")
    double camZ();

    @Accessor("intersection")
    FrustumIntersection frustumIntersection();

    @Invoker("cubeInFrustum")
    boolean invokeCubeInFrustum(double minX, double minY, double minZ, double maxX, double maxY, double maxZ);
}
