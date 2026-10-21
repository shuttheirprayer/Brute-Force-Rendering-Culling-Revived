package misanthropy.brute_force_culling_revived.api.impl;

public interface IVegetationSection {
    boolean bruteForceRenderingRevived$hasVegetation();

    boolean bruteForceRenderingRevived$isVegetationThinned();

    void bruteForceRenderingRevived$setVegetation(boolean hasVegetation, boolean thinned);
}
