package com.exiledradio.netherupdatebackport.lookfeel;

/**
 * What patches P3 and P4 ({@code docs/PATCHES.md}) call. Both are client-side render code; the descriptors are
 * primitives only, so this class loads anywhere.
 */
public final class LookHooks {

    private LookHooks() {
    }

    /**
     * P3, {@code EntityRenderer.setupCameraTransform}: the portal and nausea wobble's strength. 1.16.2's
     * {@code GameRenderer.renderLevel} multiplies it by Distortion Effects twice.
     */
    public static float portalWobble(float strength) {
        float scale = LookAndFeel.screenEffectScale();
        return strength * scale * scale;
    }

    /**
     * P4, {@code RenderGlobal.setupTerrain}: the entity render distance weight. 1.16's
     * {@code LevelRenderer.renderLevel} multiplies vanilla's clamp(render distance / 8, 1, 2.5) by Entity Distance.
     */
    public static double entityDistance(double weight) {
        return weight * LookAndFeel.ENTITY_DISTANCE.get();
    }
}
