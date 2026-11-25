package misanthropy.brute_force_culling_revived.util;

import me.jellysquid.mods.sodium.client.render.viewport.CameraTransform;
import me.jellysquid.mods.sodium.client.render.viewport.Viewport;
import me.jellysquid.mods.sodium.client.render.viewport.frustum.Frustum;
import me.jellysquid.mods.sodium.client.render.viewport.frustum.SimpleFrustum;
import misanthropy.brute_force_culling_revived.mixin.AccessorFrustum;
import misanthropy.brute_force_culling_revived.mixin.sodium.AccessorViewport;
import net.minecraft.client.Minecraft;
import org.joml.Vector3d;
import org.joml.Vector3f;

final class TurnMargin {
    private static final float SEARCH_SECONDS = 0.02F;
    private static final float SMALLEST = 0.15F;
    private static final float LARGEST = 0.8F;

    private static final Vector3f LAST_LOOK = new Vector3f();
    private static long lastNanos;
    private static float turnSpeed;

    private TurnMargin() {
    }

    static Viewport widen(Viewport viewport, Vector3f look) {
        long now = System.nanoTime();
        float seconds = Math.min((now - lastNanos) / 1.0E9F, 0.25F);
        lastNanos = now;
        if (seconds <= 0.0F) {
            seconds = 1.0E-3F;
        }
        float turned = LAST_LOOK.lengthSquared() < 0.5F ? 0.0F : LAST_LOOK.distance(look);
        LAST_LOOK.set(look);
        turnSpeed = Math.max(turned / seconds, turnSpeed * (float) Math.exp(-seconds * 12.0F));
        float margin = Math.min(LARGEST, SMALLEST + turnSpeed * (SEARCH_SECONDS + 2.0F * seconds));
        return wide(viewport, margin, 0.0F);
    }

    static Viewport exact(Viewport viewport) {
        AccessorFrustum frustum = (AccessorFrustum) Minecraft.getInstance().levelRenderer.getFrustum();
        CameraTransform camera = viewport.getTransform();
        if (frustum == null || frustum.camX() != camera.x || frustum.camY() != camera.y || frustum.camZ() != camera.z) {
            return viewport;
        }
        return new Viewport(new SimpleFrustum(frustum.frustumIntersection()), position(viewport));
    }

    static Viewport wide(Viewport viewport, float margin, float blocks) {
        return new Viewport(new Wide(((AccessorViewport) (Object) viewport).bruteForceRenderingRevived$frustum(), margin, blocks), position(viewport));
    }

    static Vector3d position(Viewport viewport) {
        CameraTransform camera = viewport.getTransform();
        return new Vector3d(camera.x, camera.y, camera.z);
    }

    private record Wide(Frustum frustum, float margin, float blocks) implements Frustum {
        @Override
        public boolean testAab(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
            float x = minX + maxX;
            float y = minY + maxY;
            float z = minZ + maxZ;
            float extra = (float) Math.sqrt(x * x + y * y + z * z) * 0.5F * this.margin + this.blocks;
            return this.frustum.testAab(minX - extra, minY - extra, minZ - extra, maxX + extra, maxY + extra, maxZ + extra);
        }
    }
}
