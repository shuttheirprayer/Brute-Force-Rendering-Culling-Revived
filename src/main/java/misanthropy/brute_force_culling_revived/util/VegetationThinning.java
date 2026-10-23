package misanthropy.brute_force_culling_revived.util;

import misanthropy.brute_force_culling_revived.api.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import misanthropy.brute_force_culling_revived.mixin.AccessorBlockBehaviour;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.GrowingPlantBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class VegetationThinning {
    private record State(int cameraX, int cameraZ, int rangeSq) {}

    private static final Set<SoundType> PLANT_SOUNDS = Set.of(
            SoundType.GRASS, SoundType.WET_GRASS, SoundType.SWEET_BERRY_BUSH, SoundType.FUNGUS,
            SoundType.ROOTS, SoundType.NETHER_SPROUTS, SoundType.PINK_PETALS);

    private static volatile State state = new State(0, 0, -1);
    private static final ConcurrentHashMap<BlockState, Boolean> VEGETATION = new ConcurrentHashMap<>();

    private VegetationThinning() {
    }

    public static boolean update(int cameraSectionX, int cameraSectionZ, int renderDistance) {
        int range = Config.getVegetationThinningRange(renderDistance);
        State next = new State(cameraSectionX, cameraSectionZ, range < 0 ? -1 : range * range);
        if (next.equals(state)) return false;
        state = next;
        return true;
    }

    public static boolean isFar(int sectionX, int sectionZ) {
        State s = state;
        if (s.rangeSq < 0) return false;
        int dx = sectionX - s.cameraX;
        int dz = sectionZ - s.cameraZ;
        return dx * dx + dz * dz > s.rangeSq;
    }

    public static void forgetVegetation() {
        VEGETATION.clear();
    }

    public static boolean isVegetation(BlockState blockState) {
        Boolean known = VEGETATION.get(blockState);
        if (known != null) return known;
        boolean vegetation = classify(blockState);
        VEGETATION.put(blockState, vegetation);
        return vegetation;
    }

    private static boolean classify(BlockState blockState) {
        Block block = blockState.getBlock();
        if (block instanceof CropBlock || block instanceof StemBlock || block instanceof SaplingBlock
                || block instanceof NetherWartBlock || block instanceof SugarCaneBlock
                || block instanceof GrowingPlantBlock || block instanceof VineBlock || block instanceof MultifaceBlock) {
            return false;
        }
        if (block instanceof BushBlock || blockState.is(BlockTags.SMALL_FLOWERS) || blockState.is(BlockTags.TALL_FLOWERS)) {
            return true;
        }
        return !blockState.hasBlockEntity()
                && PLANT_SOUNDS.contains(blockState.getSoundType())
                && !((AccessorBlockBehaviour) block).bruteForceRenderingRevived$hasCollision();
    }

    public static boolean isHidden(BlockState blockState, BlockPos pos) {
        int y = pos.getY();
        if (blockState.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                && blockState.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) {
            y--;
        }
        return (Mth.getSeed(pos.getX(), y, pos.getZ()) & 1L) == 0L;
    }
}
