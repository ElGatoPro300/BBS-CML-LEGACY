package mchorse.bbs_mod.client.screen;

/**
 * Shared layout space for film screen overlays (Image, Subtitle, Video, Hotbar, BossBar).
 * <p>
 * Clip size percentages and pixel offsets are interpreted against a fixed
 * {@link #REFERENCE_WIDTH}×{@link #REFERENCE_HEIGHT} frame, then mapped to the current
 * export/window size with a uniform fit scale. Changing the 3D viewport / export aspect
 * therefore preserves each overlay's authored aspect ratio.
 */
public final class ScreenOverlayLayout
{
    public static final float REFERENCE_WIDTH = 1920F;
    public static final float REFERENCE_HEIGHT = 1080F;

    private ScreenOverlayLayout()
    {}

    /**
     * Uniform scale that fits the reference frame inside {@code frameW}×{@code frameH}
     * (same contract as Hotbar/BossBar).
     */
    public static float resolutionScale(int frameW, int frameH)
    {
        if (frameW <= 0 || frameH <= 0)
        {
            return 1F;
        }

        return Math.max(0.05F, Math.min(frameW / REFERENCE_WIDTH, frameH / REFERENCE_HEIGHT));
    }

    /**
     * @param widthPercent fraction 0..1 of {@link #REFERENCE_WIDTH}
     */
    public static float sizeX(float widthPercent, float scale)
    {
        return REFERENCE_WIDTH * widthPercent * scale;
    }

    /**
     * @param heightPercent fraction 0..1 of {@link #REFERENCE_HEIGHT}
     */
    public static float sizeY(float heightPercent, float scale)
    {
        return REFERENCE_HEIGHT * heightPercent * scale;
    }

    /** Reference-space pixel offset → current frame pixels. */
    public static float offset(float referencePixels, float scale)
    {
        return referencePixels * scale;
    }

    /**
     * Screen X from normalized window anchor (current frame) plus reference-space offset.
     */
    public static float posX(int frameW, float windowX, float offsetX, float scale)
    {
        return frameW * windowX + offsetX * scale;
    }

    /**
     * Screen Y from normalized window anchor (current frame) plus reference-space offset.
     */
    public static float posY(int frameH, float windowY, float offsetY, float scale)
    {
        return frameH * windowY + offsetY * scale;
    }

    /**
     * Height% (0..100) that keeps {@code textureW}×{@code textureH} aspect for a given width%
     * when both are interpreted against the reference frame.
     */
    public static double heightPercentForWidth(double widthPercent, int textureW, int textureH)
    {
        if (textureW <= 0 || textureH <= 0)
        {
            return widthPercent;
        }

        return widthPercent * REFERENCE_WIDTH * textureH / ((double) textureW * REFERENCE_HEIGHT);
    }

    /**
     * Width% (0..100) that keeps texture aspect for a given height% against the reference frame.
     */
    public static double widthPercentForHeight(double heightPercent, int textureW, int textureH)
    {
        if (textureW <= 0 || textureH <= 0)
        {
            return heightPercent;
        }

        return heightPercent * textureW * REFERENCE_HEIGHT / ((double) textureH * REFERENCE_WIDTH);
    }
}
