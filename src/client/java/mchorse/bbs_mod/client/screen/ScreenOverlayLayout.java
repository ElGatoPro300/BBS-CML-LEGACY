package mchorse.bbs_mod.client.screen;

/**
 * Shared layout helpers for film screen overlays against a fixed
 * {@link #REFERENCE_WIDTH}×{@link #REFERENCE_HEIGHT} authoring space.
 * <p>
 * Composition overlays (Image, Subtitle) are placed in a virtual 1920×1080 stage
 * centered inside the current export frame, so landscape↔portrait keeps size and
 * relative position. Edge HUD (Hotbar, BossBar) uses native pixel size anchored to
 * the full frame (no fit-shrink).
 * <p>
 * Video clips size against the <em>current</em> frame
 * ({@code WaterMediaVideoRenderer}) so 100%×100% can fill the export resolution.
 */
public final class ScreenOverlayLayout
{
    public static final float REFERENCE_WIDTH = 1920F;
    public static final float REFERENCE_HEIGHT = 1080F;

    private ScreenOverlayLayout()
    {}

    /**
     * Legacy uniform fit scale. Prefer stage/native helpers for new overlay code.
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
     * Minecraft-style auto GUI scale for a framebuffer of {@code frameW}×{@code frameH}
     * (largest integer with scaled size ≥ 320×240). Hotbar/BossBar multiply by this so
     * default scale 1 matches in-game HUD size when drawing in export-pixel ortho.
     */
    public static float hudGuiScale(int frameW, int frameH)
    {
        if (frameW <= 0 || frameH <= 0)
        {
            return 1F;
        }

        int scale = 1;

        while (frameW / (scale + 1) >= 320 && frameH / (scale + 1) >= 240)
        {
            scale += 1;
        }

        return scale;
    }

    /** Left edge of the centered 1920×1080 stage inside {@code frameW}. */
    public static float stageOriginX(int frameW)
    {
        return (frameW - REFERENCE_WIDTH) * 0.5F;
    }

    /** Top edge of the centered 1920×1080 stage inside {@code frameH}. */
    public static float stageOriginY(int frameH)
    {
        return (frameH - REFERENCE_HEIGHT) * 0.5F;
    }

    /**
     * Absolute width in reference pixels.
     *
     * @param widthPercent fraction 0..1 of {@link #REFERENCE_WIDTH}
     */
    public static float referenceSizeX(float widthPercent)
    {
        return REFERENCE_WIDTH * widthPercent;
    }

    /**
     * Absolute height in reference pixels.
     *
     * @param heightPercent fraction 0..1 of {@link #REFERENCE_HEIGHT}
     */
    public static float referenceSizeY(float heightPercent)
    {
        return REFERENCE_HEIGHT * heightPercent;
    }

    /**
     * @param widthPercent fraction 0..1 of {@link #REFERENCE_WIDTH}
     */
    public static float sizeX(float widthPercent, float scale)
    {
        return referenceSizeX(widthPercent) * scale;
    }

    /**
     * @param heightPercent fraction 0..1 of {@link #REFERENCE_HEIGHT}
     */
    public static float sizeY(float heightPercent, float scale)
    {
        return referenceSizeY(heightPercent) * scale;
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
     * Position inside the centered 1920×1080 stage (composition overlays).
     */
    public static float stagePosX(int frameW, float windowX, float offsetX)
    {
        return stageOriginX(frameW) + REFERENCE_WIDTH * windowX + offsetX;
    }

    /**
     * Position inside the centered 1920×1080 stage (composition overlays).
     */
    public static float stagePosY(int frameH, float windowY, float offsetY)
    {
        return stageOriginY(frameH) + REFERENCE_HEIGHT * windowY + offsetY;
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
