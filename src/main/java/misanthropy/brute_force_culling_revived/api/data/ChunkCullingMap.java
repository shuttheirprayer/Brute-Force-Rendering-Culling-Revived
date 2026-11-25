package misanthropy.brute_force_culling_revived.api.data;

import misanthropy.brute_force_culling_revived.api.Config;
import misanthropy.brute_force_culling_revived.api.CullingStateManager;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class ChunkCullingMap extends CullingMap {
    private int renderDistance = 0;
    private int spacePartitionSize = 0;
    private int drawnX;
    private int drawnZ;
    private volatile View view;

    private record View(byte[] data, int cameraX, int cameraZ) {
    }

    public ChunkCullingMap(int width, int height) {
        super(width, height);
    }

    @Override
    protected boolean shouldUpdate() {
        return CullingStateManager.continueUpdateChunk();
    }

    @Override
    int configDelayCount() {
        return Config.getDepthUpdateDelay();
    }

    @Override
    int bindFrameBufferId() {
        return CullingStateManager.CHUNK_CULLING_MAP_TARGET.frameBufferId;
    }

    public void generateIndex(int renderDistance) {
        this.renderDistance = renderDistance;
        this.spacePartitionSize = 2 * renderDistance + 1;
    }

    @Override
    public void markDrawn() {
        super.markDrawn();
        Vec3 camera = CullingStateManager.CAMERA.getPosition();
        this.drawnX = Mth.floor(camera.x) >> 4;
        this.drawnZ = Mth.floor(camera.z) >> 4;
    }

    @Override
    public void readData() {
        super.readData();
        this.view = new View(this.data, this.drawnX, this.drawnZ);
    }

    public boolean isChunkOffsetCameraVisible(int x, int y, int z, boolean checkForChunk) {
        View view = this.view;
        if (view == null) {
            return true;
        }
        int posX = (x >> 4) - view.cameraX + renderDistance;
        int posY = CullingStateManager.mapChunkY(y);
        int posZ = (z >> 4) - view.cameraZ + renderDistance;
        int range = CullingStateManager.LEVEL_SECTION_RANGE;
        if (posX < 0 || posX >= spacePartitionSize || posZ < 0 || posZ >= spacePartitionSize || posY < 0 || posY >= range) {
            return true;
        }
        int index = 1 + (((posX * spacePartitionSize + posZ) * range + posY) << 2);
        return index >= view.data.length || (view.data[index] & 0xFF) > (checkForChunk ? 0 : 127);
    }
}