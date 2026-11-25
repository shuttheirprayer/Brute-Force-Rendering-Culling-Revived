package misanthropy.brute_force_culling_revived.util;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkUpdateType;
import me.jellysquid.mods.sodium.client.render.chunk.LocalSectionIndex;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.lists.ChunkRenderList;
import me.jellysquid.mods.sodium.client.render.chunk.lists.SortedRenderLists;
import me.jellysquid.mods.sodium.client.render.chunk.lists.VisibleChunkCollector;
import me.jellysquid.mods.sodium.client.render.chunk.occlusion.OcclusionCuller;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;
import me.jellysquid.mods.sodium.client.render.viewport.CameraTransform;
import me.jellysquid.mods.sodium.client.render.viewport.Viewport;
import misanthropy.brute_force_culling_revived.api.CullingStateManager;
import misanthropy.brute_force_culling_revived.api.impl.ICollectorAccessor;
import misanthropy.brute_force_culling_revived.api.impl.IRenderSectionVisibility;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.*;

public class SodiumSectionAsyncUtil {
    private static final int AROUND_INTERVAL = 4;
    private static final float AROUND_DISTANCE = 64.0F;

    private static int frame = 0;
    private static OcclusionCuller occlusionCuller;
    private static Viewport viewport;
    private static final float REUSE_TURN = 0.04F;
    private static final float REUSE_BLOCKS = 1.0F;
    private static Viewport exactViewport;
    private static final Vector3f LOOK = new Vector3f();
    private static int searches;
    private static float searchDistance;
    private static boolean useOcclusionCulling;
    private static Viewport shadowViewport;
    private static float shadowSearchDistance;
    private static boolean shadowUseOcclusionCulling;
    private static @Nullable VisibleChunkCollector collector;
    private static @Nullable VisibleChunkCollector shadowCollector;

    private static final Object LOCK = new Object();
    public static final Object TRAVERSAL_LOCK = new Object();
    private static volatile int generation = 0;

    public static boolean renderingEntities;
    public static boolean needSyncRebuild;

    public static void fromSectionManager(Long2ReferenceMap<RenderSection> sections, Level world) {
        synchronized (LOCK) {
            SodiumSectionAsyncUtil.occlusionCuller = new OcclusionCuller(sections, world);
        }
    }

    public static void reset() {
        synchronized (LOCK) {
            generation++;
            collector = null;
            shadowCollector = null;
            viewport = null;
            exactViewport = null;
            shadowViewport = null;
            occlusionCuller = null;
            needSyncRebuild = false;
        }
    }

    public static void asyncSearchRebuildSection() {
        int currentGeneration;
        OcclusionCuller culler;
        Viewport localShadowViewport;
        float localShadowSearchDistance;
        boolean localShadowUseOcclusionCulling;
        Viewport localViewport;
        float localSearchDistance;
        boolean localUseOcclusionCulling;

        synchronized (LOCK) {
            currentGeneration = generation;
            culler = occlusionCuller;
            localShadowViewport = shadowViewport;
            localShadowSearchDistance = shadowSearchDistance;
            localShadowUseOcclusionCulling = shadowUseOcclusionCulling;
            localViewport = viewport;
            localSearchDistance = searchDistance;
            localUseOcclusionCulling = useOcclusionCulling;
        }

        if (CullingStateManager.needPauseRebuild() || culler == null) {
            return;
        }

        VisibleChunkCollector sColl = null;

        if (CullingStateManager.enabledShader() && localShadowViewport != null) {
            frame++;
            CullingStateManager.useOcclusionCulling = false;
            try {
                sColl = new AsynchronousChunkCollector(frame, false);
                synchronized (TRAVERSAL_LOCK) {
                    culler.findVisible(sColl, localShadowViewport, localShadowSearchDistance, localShadowUseOcclusionCulling, frame);
                }
            } finally {
                CullingStateManager.useOcclusionCulling = true;
            }
        }

        VisibleChunkCollector mainColl = null;
        boolean localNeedSyncRebuild = false;

        if (localViewport != null) {
            mainColl = CullingStateManager.checkCulling ?
                    new DebugChunkCollector(frame) : new AsynchronousChunkCollector(frame, true);

            if (++searches % AROUND_INTERVAL == 0) {
                frame++;
                ICollectorAccessor target = (ICollectorAccessor) mainColl;
                Viewport around = new Viewport((minX, minY, minZ, maxX, maxY, maxZ) -> true, TurnMargin.position(localViewport));
                synchronized (TRAVERSAL_LOCK) {
                    culler.findVisible((section, visible) -> target.bruteForceRenderingRevived$addAsyncToRebuildLists(section),
                            around, Math.min(localSearchDistance, AROUND_DISTANCE), localUseOcclusionCulling, frame);
                }
            }

            synchronized (TRAVERSAL_LOCK) {
                frame++;
                culler.findVisible(mainColl, localViewport, localSearchDistance, localUseOcclusionCulling, frame);
            }

            localNeedSyncRebuild = hasWork(mainColl);
        }

        synchronized (LOCK) {
            if (currentGeneration == generation) {
                SodiumSectionAsyncUtil.collector = mainColl;
                SodiumSectionAsyncUtil.shadowCollector = sColl;
                SodiumSectionAsyncUtil.needSyncRebuild = localNeedSyncRebuild;
            }
        }
    }

    private static boolean hasWork(VisibleChunkCollector collector) {
        for (ArrayDeque<RenderSection> queue : collector.getRebuildLists().values()) {
            if (!queue.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public static void pauseAsync() {
        synchronized (LOCK) {
            SodiumSectionAsyncUtil.collector = null;
            SodiumSectionAsyncUtil.shadowCollector = null;
        }
    }

    public static void update(Viewport viewport, float dist, boolean culling) {
        synchronized (LOCK) {
            if (CullingStateManager.renderingShader()) {
                SodiumSectionAsyncUtil.shadowViewport = viewport;
                SodiumSectionAsyncUtil.shadowSearchDistance = dist;
                SodiumSectionAsyncUtil.shadowUseOcclusionCulling = culling;
            } else {
                SodiumSectionAsyncUtil.exactViewport = TurnMargin.exact(viewport);
                LOOK.set(Minecraft.getInstance().gameRenderer.getMainCamera().getLookVector());
                SodiumSectionAsyncUtil.viewport = TurnMargin.widen(exactViewport, LOOK);
                SodiumSectionAsyncUtil.searchDistance = dist;
                SodiumSectionAsyncUtil.useOcclusionCulling = culling;
            }
        }
    }

    public static @Nullable VisibleChunkCollector getChunkCollector() {
        synchronized (LOCK) {
            return collector;
        }
    }

    public static @Nullable VisibleChunkCollector getShadowCollector() {
        synchronized (LOCK) {
            return shadowCollector;
        }
    }

    public static @Nullable VisibleChunkCollector getActiveCollector() {
        return CullingStateManager.renderingShader() ? getShadowCollector() : getChunkCollector();
    }

    public static boolean hasCuller() {
        synchronized (LOCK) {
            return occlusionCuller != null;
        }
    }

    private static ChunkRenderList regionList(@NotNull Map<RenderRegion, ChunkRenderList> map, @NotNull VisibleChunkCollector owner, @NotNull RenderRegion region) {
        ChunkRenderList list = map.get(region);
        if (list == null) {
            list = new ChunkRenderList(region);
            map.put(region, list);
            ((ICollectorAccessor) owner).bruteForceRenderingRevived$addRenderList(list);
        }
        return list;
    }

    public static class AsynchronousChunkCollector extends VisibleChunkCollector {
        private final Map<RenderRegion, RegionSections> regions = new HashMap<>();
        private final List<RegionSections> regionOrder = new ArrayList<>();
        private final boolean trim;
        private final EnumMap<ChunkUpdateType, ArrayDeque<RenderSection>> syncRebuildLists;
        private static final ChunkUpdateType[] UPDATE_TYPES = ChunkUpdateType.values();
        private static final EnumMap<ChunkUpdateType, ArrayDeque<RenderSection>> EMPTY_LIST = new EnumMap<>(ChunkUpdateType.class);

        static {
            for (ChunkUpdateType type : UPDATE_TYPES) {
                EMPTY_LIST.put(type, new ArrayDeque<>());
            }
        }

        private boolean sent;
        private @Nullable CameraTransform trimmedAt;
        private final Vector3f trimmedLook = new Vector3f();

        public AsynchronousChunkCollector(int frame, boolean trim) {
            super(frame);
            this.trim = trim;
            this.syncRebuildLists = new EnumMap<>(ChunkUpdateType.class);
            for (ChunkUpdateType type : UPDATE_TYPES) {
                this.syncRebuildLists.put(type, new ArrayDeque<>());
            }
        }

        @Override
        public void visit(@NotNull RenderSection section, boolean visible) {
            if (visible && (section.getFlags() != 0 || section.getPendingUpdate() != null || section.getBuildCancellationToken() != null)) {
                RenderRegion region = section.getRegion();
                RegionSections group = regions.get(region);
                if (group == null) {
                    group = new RegionSections(region);
                    regions.put(region, group);
                    regionOrder.add(group);
                }
                group.add(section);
            }
            ((ICollectorAccessor) this).bruteForceRenderingRevived$addAsyncToRebuildLists(section);
        }

        @Override
        public SortedRenderLists createRenderLists() {
            Viewport exact = trim ? exactViewport : null;
            if (exact != null) {
                CameraTransform camera = exact.getTransform();
                if (trimmedAt != null && trimmedLook.distanceSquared(LOOK) < REUSE_TURN * REUSE_TURN && within(camera, trimmedAt)) {
                    return super.createRenderLists();
                }
                trimmedAt = camera;
                trimmedLook.set(LOOK);
                exact = TurnMargin.wide(exact, REUSE_TURN * 1.5F, REUSE_BLOCKS);
            }
            ICollectorAccessor lists = (ICollectorAccessor) this;
            lists.bruteForceRenderingRevived$clearRenderLists();
            for (int r = 0, regionCount = regionOrder.size(); r < regionCount; r++) {
                ChunkRenderList list = regionOrder.get(r).visible(exact);
                if (list.size() > 0) {
                    lists.bruteForceRenderingRevived$addRenderList(list);
                }
            }
            return super.createRenderLists();
        }

        private static boolean within(CameraTransform a, CameraTransform b) {
            double x = a.x - b.x;
            double y = a.y - b.y;
            double z = a.z - b.z;
            return x * x + y * y + z * z < REUSE_BLOCKS * REUSE_BLOCKS;
        }

        private static final class RegionSections {
            private static final float EDGE = 1.125F;
            private static final float SECTION = 8.0F + EDGE;

            private final RenderRegion region;
            private final ChunkRenderList list;
            private byte[] indices = new byte[64];
            private int count;

            private RegionSections(RenderRegion region) {
                this.region = region;
                this.list = new ChunkRenderList(region);
            }

            private void add(RenderSection section) {
                if (this.count == this.indices.length) {
                    this.indices = Arrays.copyOf(this.indices, RenderRegion.REGION_SIZE);
                }
                this.indices[this.count++] = (byte) section.getSectionIndex();
            }

            private ChunkRenderList visible(@Nullable Viewport exact) {
                ChunkRenderList list = this.list;
                list.reset(0);
                RenderRegion region = this.region;
                if (exact != null && !exact.isBoxVisible(region.getCenterX(), region.getCenterY(), region.getCenterZ(),
                        RenderRegion.REGION_WIDTH * 8.0F + EDGE, RenderRegion.REGION_HEIGHT * 8.0F + EDGE, RenderRegion.REGION_LENGTH * 8.0F + EDGE)) {
                    return list;
                }
                int x = region.getOriginX() + 8;
                int y = region.getOriginY() + 8;
                int z = region.getOriginZ() + 8;
                byte[] indices = this.indices;
                for (int i = 0, count = this.count; i < count; i++) {
                    int index = indices[i] & 0xFF;
                    if (exact != null && !exact.isBoxVisible(x + (LocalSectionIndex.unpackX(index) << 4),
                            y + (LocalSectionIndex.unpackY(index) << 4), z + (LocalSectionIndex.unpackZ(index) << 4), SECTION)) {
                        continue;
                    }
                    RenderSection section = region.getSection(index);
                    if (section != null && section.getFlags() != 0 && !section.isDisposed()) {
                        list.add(section);
                    }
                }
                return list;
            }
        }

        @Override
        public Map<ChunkUpdateType, ArrayDeque<RenderSection>> getRebuildLists() {
            if (!RenderSystem.isOnRenderThread()) return super.getRebuildLists();
            if (sent) return EMPTY_LIST;
            sent = true;

            if (CullingStateManager.needPauseRebuild()) return syncRebuildLists;

            for (Map.Entry<ChunkUpdateType, ArrayDeque<RenderSection>> entry : super.getRebuildLists().entrySet()) {
                ArrayDeque<RenderSection> target = syncRebuildLists.get(entry.getKey());
                for (RenderSection s : entry.getValue()) {
                    if (!s.isDisposed() && s.getBuildCancellationToken() == null) {
                        target.add(s);
                    }
                }
            }
            return syncRebuildLists;
        }
    }

    public static class DebugChunkCollector extends VisibleChunkCollector {
        private final Map<RenderRegion, ChunkRenderList> renderListMap = new HashMap<>();

        public DebugChunkCollector(int frame) {
            super(frame);
        }

        @Override
        public void visit(@NotNull RenderSection section, boolean visible) {
            if (!visible || section.getFlags() == 0) return;

            boolean shouldCull = !CullingStateManager.shouldRenderChunk((IRenderSectionVisibility) section, true);
            if (shouldCull != CullingStateManager.checkCulling) return;

            ChunkRenderList list = regionList(renderListMap, this, section.getRegion());
            if (list.size() < 256) list.add(section);
        }
    }
}