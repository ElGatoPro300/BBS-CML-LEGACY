package mchorse.bbs_mod.ui.dashboard.welcome;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.utils.colors.Colors;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;

import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;

import java.util.Random;

/**
 * Deep Space & "Pillars of Creation" Cosmic Nebula Background Renderer.
 *
 * <p>Pure GPU-batched procedural space scenery featuring radiant emission nebulae,
 * towering interstellar dust pillars, dense twinkling multi-color starfields,
 * diffraction-spike hero stars, drifting cosmic stardust, and gravitational cursor aura.</p>
 */
public class UINostalgicBackground
{
    private static final int STAR_COUNT = 130;
    private static final int DUST_PARTICLE_COUNT = 45;
    private static final int HERO_STAR_COUNT = 9;

    private static final int GLOW_GRID = 16;
    private static final float[][] GLOW_WEIGHTS = new float[GLOW_GRID + 1][GLOW_GRID + 1];

    static
    {
        float half = GLOW_GRID * 0.5F;
        float invHalf = 1.0F / half;

        for (int r = 0; r <= GLOW_GRID; r++)
        {
            float dy = (r - half) * invHalf;
            float dySq = dy * dy;

            for (int c = 0; c <= GLOW_GRID; c++)
            {
                float dx = (c - half) * invHalf;
                float distSq = dx * dx + dySq;

                if (distSq < 1.0F)
                {
                    float d = (float) Math.sqrt(distSq);
                    float f = 1.0F - d;
                    GLOW_WEIGHTS[r][c] = f * f * (3.0F - 2.0F * f);
                }
                else
                {
                    GLOW_WEIGHTS[r][c] = 0.0F;
                }
            }
        }
    }

    private float parallaxX;
    private float parallaxY;
    private long lastTime = -1L;

    private final Star[] stars = new Star[STAR_COUNT];
    private final HeroStar[] heroStars = new HeroStar[HERO_STAR_COUNT];
    private final CosmicDust[] dustParticles = new CosmicDust[DUST_PARTICLE_COUNT];

    public UINostalgicBackground()
    {
        Random random = new Random(0x50ACE3L);

        /* 1. Background & Midground Starfield */
        int[] starColors = {0xffffff, 0xd8eeff, 0x98d4ff, 0xffe2a4, 0xffc490, 0xffa8c8};

        for (int i = 0; i < STAR_COUNT; i++)
        {
            Star s = new Star();

            s.u = random.nextFloat();
            s.v = random.nextFloat();
            s.depth = 0.20F + random.nextFloat() * 0.80F;
            s.size = 0.8F + random.nextFloat() * 1.5F;
            s.twinkleSpeed = 0.8F + random.nextFloat() * 2.2F;
            s.twinklePhase = random.nextFloat() * (float) (Math.PI * 2);
            s.baseAlpha = 0.35F + random.nextFloat() * 0.55F;
            s.color = starColors[random.nextInt(starColors.length)];

            this.stars[i] = s;
        }

        /* 2. Hero Diffraction-Spike Stars (JWST/Hubble style flares) */
        float[][] heroCoords = {
            {0.14F, 0.18F, 18F, 0xfff4cc},
            {0.28F, 0.12F, 24F, 0xd4f0ff},
            {0.48F, 0.08F, 30F, 0xffffff},
            {0.82F, 0.16F, 26F, 0xffddaa},
            {0.88F, 0.28F, 22F, 0x9cd8ff},
            {0.64F, 0.35F, 20F, 0xffaacc},
            {0.36F, 0.42F, 22F, 0xd0f4ff},
            {0.18F, 0.68F, 25F, 0xffeedd},
            {0.92F, 0.75F, 20F, 0xa0e4ff}
        };

        for (int i = 0; i < HERO_STAR_COUNT; i++)
        {
            HeroStar hs = new HeroStar();
            float[] c = heroCoords[i];

            hs.u = c[0];
            hs.v = c[1];
            hs.flareRadius = c[2];
            hs.color = (int) c[3];
            hs.shimmerSpeed = 1.0F + random.nextFloat() * 1.5F;
            hs.shimmerPhase = random.nextFloat() * (float) (Math.PI * 2);

            this.heroStars[i] = hs;
        }

        /* 3. Floating Cosmic Stardust */
        for (int i = 0; i < DUST_PARTICLE_COUNT; i++)
        {
            CosmicDust d = new CosmicDust();

            d.u = random.nextFloat();
            d.v = random.nextFloat();
            d.depth = 0.25F + random.nextFloat() * 0.75F;
            d.size = 1.0F + random.nextFloat() * 2.0F;
            d.wobbleSpeed = 0.6F + random.nextFloat() * 1.0F;
            d.wobblePhase = random.nextFloat() * (float) (Math.PI * 2);
            d.wobbleAmp = 0.005F + random.nextFloat() * 0.007F;
            d.driftX = 0.0012F + random.nextFloat() * 0.0025F;
            d.driftY = -0.0010F - random.nextFloat() * 0.0020F;
            d.baseAlpha = 0.35F + random.nextFloat() * 0.45F;
            d.color = random.nextBoolean() ? 0x90e8ff : (random.nextBoolean() ? 0xffbe70 : 0xff9cd0);

            this.dustParticles[i] = d;
        }
    }

    public void render(UIContext context, Area area)
    {
        this.render(context, area, 1.0F, 1.0F);
    }

    public void render(UIContext context, Area area, float energy)
    {
        this.render(context, area, energy, energy);
    }

    public void render(UIContext context, Area area, float energy, float particleEnergy)
    {
        if (area.w <= 0 || area.h <= 0 || energy <= 0.001F)
        {
            return;
        }

        this.updateParallax(context, area);

        long now = System.currentTimeMillis();
        float timeSec = (now % 10000000L) / 1000F;

        Matrix4f mat = context.batcher.getContext().getMatrices().peek().getPositionMatrix();
        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        /* 1. Deep Cosmic Void Background */
        this.renderCosmicVoid(builder, mat, area, energy);

        /* 2. Glowing Emission & Reflection Nebulae (Amber / Crimson / Cyan clouds) */
        this.renderNebulaGlowPools(builder, mat, area, energy, timeSec);

        /* 3. Towering Pillars of Creation (Cosmic dust columns) */
        this.renderNebulaPillars(builder, mat, area, energy, timeSec);

        /* 4. Twinkling Starfield */
        this.renderStarfield(builder, mat, area, energy, timeSec);

        /* 5. Hero Stars with Diffraction Spikes */
        this.renderHeroStars(builder, mat, area, energy, timeSec);

        /* 6. Center Nebula Shadow for UI Readability */
        this.renderCenterShadow(builder, mat, area, energy);

        /* 7. Drifting Cosmic Stardust & Cursor Aura */
        this.renderCosmicDust(builder, mat, area, particleEnergy, timeSec, context);

        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        context.batcher.getContext().draw();
        drawBufferIfNotEmpty(builder);
    }

    private void updateParallax(UIContext context, Area area)
    {
        long now = System.currentTimeMillis();
        float dt = this.lastTime > 0 ? Math.min((now - this.lastTime) / 1000F, 0.1F) : 0.016F;

        this.lastTime = now;

        float targetX = (context.mouseX - area.mx()) / (float) Math.max(1, area.w / 2);
        float targetY = (context.mouseY - area.my()) / (float) Math.max(1, area.h / 2);

        targetX = Math.max(-1F, Math.min(1F, targetX));
        targetY = Math.max(-1F, Math.min(1F, targetY));

        float smoothFactor = 1.0F - (float) Math.exp(-dt * 4.0F);

        this.parallaxX += (targetX - this.parallaxX) * smoothFactor;
        this.parallaxY += (targetY - this.parallaxY) * smoothFactor;
    }

    private void renderCosmicVoid(BufferBuilder builder, Matrix4f mat, Area area, float energy)
    {
        int a = (int) (0xFF * energy);
        int spaceTop = (a << 24) | 0x08040d;
        int spaceMid = (a << 24) | 0x04050e;
        int spaceBot = (a << 24) | 0x020308;

        int midY = area.y + (int) (area.h * 0.48F);

        pushQuadPoints(builder, mat, area.x, area.y, area.ex(), midY, spaceTop, spaceTop, spaceMid, spaceMid);
        pushQuadPoints(builder, mat, area.x, midY, area.ex(), area.ey(), spaceMid, spaceMid, spaceBot, spaceBot);
    }

    private void renderNebulaGlowPools(BufferBuilder builder, Matrix4f mat, Area area, float energy, float timeSec)
    {
        float pX = this.parallaxX * 12F;
        float pY = this.parallaxY * 8F;
        float breath = 0.90F + 0.10F * (float) Math.sin(timeSec * 0.8D);

        /* 1. Fiery Amber / Crimson Hydrogen-Alpha Upper Nebula (JWST background emission) */
        int orangeAlpha = (int) (0x44 * energy * breath);
        int orangeGlow = (orangeAlpha << 24) | 0xee5511;
        int roseGlow = (orangeAlpha << 24) | 0xcc2255;
        int goldGlow = ((orangeAlpha * 2 / 3) << 24) | 0xffaa33;

        pushSmoothRadialGlow(builder, mat, area.x + area.w * 0.35F + pX, area.y + area.h * 0.15F + pY, area.h * 0.55F, orangeGlow);
        pushSmoothRadialGlow(builder, mat, area.x + area.w * 0.65F + pX, area.y + area.h * 0.12F + pY, area.h * 0.60F, roseGlow);
        pushSmoothRadialGlow(builder, mat, area.x + area.w * 0.85F + pX, area.y + area.h * 0.22F + pY, area.h * 0.45F, goldGlow);

        /* 2. Deep Interstellar Teal / Cyan / Violet Reflection Gas Pools */
        int cyanAlpha = (int) (0x38 * energy * breath);
        int cyanGlow = (cyanAlpha << 24) | 0x0088aa;
        int indigoGlow = (cyanAlpha << 24) | 0x332266;
        int violetGlow = (cyanAlpha << 24) | 0x551848;

        pushSmoothRadialGlow(builder, mat, area.x + area.w * 0.18F + pX, area.y + area.h * 0.65F + pY, area.h * 0.50F, cyanGlow);
        pushSmoothRadialGlow(builder, mat, area.x + area.w * 0.55F + pX, area.y + area.h * 0.78F + pY, area.h * 0.55F, indigoGlow);
        pushSmoothRadialGlow(builder, mat, area.x + area.w * 0.82F + pX, area.y + area.h * 0.60F + pY, area.h * 0.45F, violetGlow);
    }

    private void renderNebulaPillars(BufferBuilder builder, Matrix4f mat, Area area, float energy, float timeSec)
    {
        /* Towering diagonal dust columns rising from bottom-left to center-right */
        float pX = this.parallaxX * 20F;
        float pY = this.parallaxY * 14F;

        /* Pillar 1: Giant Central Pillar */
        this.renderSinglePillar(
            builder, mat, area, energy, timeSec,
            area.x + area.w * 0.12F + pX, area.ey() + 20F + pY,
            area.x + area.w * 0.58F + pX, area.y + area.h * 0.14F + pY,
            48F, 18F, 0xee7733, 0x33bbee
        );

        /* Pillar 2: Right Companion Finger */
        this.renderSinglePillar(
            builder, mat, area, energy, timeSec,
            area.x + area.w * 0.38F + pX, area.ey() + 20F + pY,
            area.x + area.w * 0.78F + pX, area.y + area.h * 0.38F + pY,
            38F, 14F, 0xdd6622, 0x44ddff
        );

        /* Pillar 3: Left Secondary Spire */
        this.renderSinglePillar(
            builder, mat, area, energy, timeSec,
            area.x - 10F + pX, area.ey() + 20F + pY,
            area.x + area.w * 0.22F + pX, area.y + area.h * 0.52F + pY,
            35F, 12F, 0xbb5533, 0x33aadd
        );
    }

    private void renderSinglePillar(
        BufferBuilder builder, Matrix4f mat, Area area, float energy, float timeSec,
        float x1, float y1, float x2, float y2, float baseWidth, float tipWidth,
        int rimGlowColor, int tipIonColor
    )
    {
        int segments = 8;
        float dx = x2 - x1;
        float dy = y2 - y1;

        int coreDark = ((int) (0xeb * energy) << 24) | 0x141018;
        int coreMid = ((int) (0xd8 * energy) << 24) | 0x261e2b;
        int rimColor = ((int) (0x70 * energy) << 24) | rimGlowColor;
        int tipGlow = ((int) (0x88 * energy) << 24) | tipIonColor;

        float prevCX = x1;
        float prevCY = y1;
        float prevW = baseWidth;

        for (int s = 1; s <= segments; s++)
        {
            float t = (float) s / segments;
            float curCX = x1 + dx * t + (float) Math.sin(t * Math.PI * 2.5D) * 12F;
            float curCY = y1 + dy * t;
            float curW = baseWidth + (tipWidth - baseWidth) * t;

            /* Pillar core */
            pushQuadSkewed(
                builder, mat,
                prevCX - prevW * 0.5F, prevCY, prevCX + prevW * 0.5F, prevCY,
                curCX - curW * 0.5F, curCY, curCX + curW * 0.5F, curCY,
                coreDark, coreDark, coreMid, coreMid
            );

            /* Illuminated glowing outer dust rim */
            pushQuadSkewed(
                builder, mat,
                prevCX - prevW * 0.5F - 10F, prevCY, prevCX - prevW * 0.5F, prevCY,
                curCX - curW * 0.5F - 8F, curCY, curCX - curW * 0.5F, curCY,
                0, rimColor, 0, rimColor
            );

            pushQuadSkewed(
                builder, mat,
                prevCX + prevW * 0.5F, prevCY, prevCX + prevW * 0.5F + 10F, prevCY,
                curCX + curW * 0.5F, curCY, curCX + curW * 0.5F + 8F, curCY,
                rimColor, 0, rimColor, 0
            );

            prevCX = curCX;
            prevCY = curCY;
            prevW = curW;
        }

        /* Glowing ionization fingertip at pillar summit */
        pushSmoothRadialGlow(builder, mat, x2, y2, tipWidth * 2.2F, tipGlow);
    }

    private void renderStarfield(BufferBuilder builder, Matrix4f mat, Area area, float energy, float timeSec)
    {
        for (int i = 0; i < STAR_COUNT; i++)
        {
            Star s = this.stars[i];

            float x = area.x + s.u * area.w + this.parallaxX * (s.depth * 25F);
            float y = area.y + s.v * area.h + this.parallaxY * (s.depth * 18F);

            if (x < area.x || x > area.ex() || y < area.y || y > area.ey())
            {
                continue;
            }

            float twinkle = 0.55F + 0.45F * (float) Math.sin(timeSec * s.twinkleSpeed + s.twinklePhase);
            float starAlpha = s.baseAlpha * twinkle * energy;
            int aInt = (int) (starAlpha * 255F);

            if (aInt <= 0) continue;

            int color = (aInt << 24) | s.color;
            float sz = s.size;

            pushQuad(builder, mat, x - sz * 0.5F, y - sz * 0.5F, sz, sz, color, color, color, color);
        }
    }

    private void renderHeroStars(BufferBuilder builder, Matrix4f mat, Area area, float energy, float timeSec)
    {
        for (int i = 0; i < HERO_STAR_COUNT; i++)
        {
            HeroStar hs = this.heroStars[i];

            float x = area.x + hs.u * area.w + this.parallaxX * 32F;
            float y = area.y + hs.v * area.h + this.parallaxY * 24F;

            float shimmer = 0.85F + 0.15F * (float) Math.sin(timeSec * hs.shimmerSpeed + hs.shimmerPhase);
            int starAlpha = (int) (0xee * energy * shimmer);
            int starColor = (starAlpha << 24) | hs.color;
            int spikeColor = ((starAlpha / 2) << 24) | hs.color;
            int haloColor = ((starAlpha / 5) << 24) | hs.color;

            float r = hs.flareRadius;

            /* Soft radial halo */
            pushSmoothRadialGlow(builder, mat, x, y, r * 0.75F, haloColor);

            /* Bright Diamond Core */
            pushQuad(builder, mat, x - 2F, y - 2F, 4F, 4F, starColor, starColor, starColor, starColor);

            /* JWST Diffraction Cross Spikes */
            float spikeThickness = 0.8F;

            /* Horizontal spike */
            pushQuadSkewed(builder, mat, x - r, y - spikeThickness, x + r, y - spikeThickness, x - r, y + spikeThickness, x + r, y + spikeThickness, 0, 0, spikeColor, spikeColor);

            /* Vertical spike */
            pushQuadSkewed(builder, mat, x - spikeThickness, y - r, x + spikeThickness, y - r, x - spikeThickness, y + r, x + spikeThickness, y + r, 0, 0, spikeColor, spikeColor);

            /* Diagonal micro-spikes (45-degree angle) */
            float diagR = r * 0.45F;
            pushQuad(builder, mat, x - diagR * 0.5F, y - diagR * 0.5F, diagR, diagR, haloColor, 0, 0, haloColor);
        }
    }

    private void renderCenterShadow(BufferBuilder builder, Matrix4f mat, Area area, float energy)
    {
        /* Smooth center darkening ensuring high contrast for the onboarding card */
        int shadowAlpha = (int) (0x66 * energy);

        if (shadowAlpha > 0)
        {
            int shadowColor = (shadowAlpha << 24) | 0x020306;
            pushSmoothRadialGlow(builder, mat, area.mx(), area.my(), area.w * 0.42F, shadowColor);
        }
    }

    private void renderCosmicDust(BufferBuilder builder, Matrix4f mat, Area area, float energy, float timeSec, UIContext context)
    {
        if (energy <= 0.001F) return;

        float wrapPad = 30F;
        float minX = area.x - wrapPad;
        float maxX = area.ex() + wrapPad;
        float totalW = maxX - minX;

        float minY = area.y - wrapPad;
        float maxY = area.ey() + wrapPad;
        float totalH = maxY - minY;

        float exitDy = (1.0F - energy) * 16.0F;

        /* 1. Floating Cosmic Stardust Motes */
        for (int i = 0; i < DUST_PARTICLE_COUNT; i++)
        {
            CosmicDust d = this.dustParticles[i];

            float wobble = (float) Math.sin(timeSec * d.wobbleSpeed + d.wobblePhase) * d.wobbleAmp;
            float normX = ((d.u + timeSec * d.driftX) % 1.0F + 1.0F) % 1.0F;
            float normY = ((d.v + timeSec * d.driftY + wobble) % 1.0F + 1.0F) % 1.0F;

            float x = area.x + normX * area.w + this.parallaxX * (d.depth * 30F);
            float y = area.y + normY * area.h + this.parallaxY * (d.depth * 20F) + exitDy * (0.6F + 0.4F * d.depth);

            while (x < minX) x += totalW;
            while (x > maxX) x -= totalW;

            while (y < minY) y += totalH;
            while (y > maxY) y -= totalH;

            float rawTwinkle = 0.65F + 0.35F * (float) Math.sin(timeSec * 1.5F + d.wobblePhase);
            float twinkle = energy < 1.0F ? (rawTwinkle * energy + 0.85F * (1.0F - energy)) : rawTwinkle;

            float alphaFactor = d.baseAlpha * twinkle * (0.6F + 0.4F * d.depth) * energy;
            int alphaInt = (int) (alphaFactor * 255F);

            if (alphaInt <= 0) continue;

            int pColor = (alphaInt << 24) | d.color;
            float sz = d.size * (0.7F + 0.3F * energy);

            if (sz > 1.6F)
            {
                int haloColor = ((alphaInt / 3) << 24) | 0x77bbff;
                pushQuad(builder, mat, x - 1.2F, y - 1.2F, sz + 2.4F, sz + 2.4F, haloColor, haloColor, haloColor, haloColor);
            }

            pushQuad(builder, mat, x, y, sz, sz, pColor, pColor, pColor, pColor);
        }

        /* 2. Interactive Gravitational Lensing Halo under cursor */
        if (context.mouseX >= area.x - 100 && context.mouseX <= area.ex() + 100
            && context.mouseY >= area.y - 100 && context.mouseY <= area.ey() + 100)
        {
            int haloAlpha = (int) (0x22 * energy);

            if (haloAlpha > 0)
            {
                int haloColor = (haloAlpha << 24) | 0x44aaff;
                pushSmoothRadialGlow(builder, mat, context.mouseX, context.mouseY, 85F, haloColor);
            }
        }
    }

    private static void drawBufferIfNotEmpty(BufferBuilder builder)
    {
        try
        {
            BufferRenderer.drawWithGlobalProgram(builder.end());
        }
        catch (IllegalStateException ignored)
        {}
    }

    private static void pushQuad(BufferBuilder builder, Matrix4f mat, float x, float y, float w, float h, int cTopLeft, int cTopRight, int cBotLeft, int cBotRight)
    {
        builder.vertex(mat, x, y, 0).color(cTopLeft);
        builder.vertex(mat, x, y + h, 0).color(cBotLeft);
        builder.vertex(mat, x + w, y + h, 0).color(cBotRight);
        builder.vertex(mat, x + w, y, 0).color(cTopRight);
    }

    private static void pushQuadPoints(BufferBuilder builder, Matrix4f mat, float x1, float y1, float x2, float y2, int cTopLeft, int cTopRight, int cBotLeft, int cBotRight)
    {
        builder.vertex(mat, x1, y1, 0).color(cTopLeft);
        builder.vertex(mat, x1, y2, 0).color(cBotLeft);
        builder.vertex(mat, x2, y2, 0).color(cBotRight);
        builder.vertex(mat, x2, y1, 0).color(cTopRight);
    }

    private static void pushQuadSkewed(BufferBuilder builder, Matrix4f mat, float xTL, float yTL, float xTR, float yTR, float xBL, float yBL, float xBR, float yBR, int cTL, int cTR, int cBL, int cBR)
    {
        builder.vertex(mat, xTL, yTL, 0).color(cTL);
        builder.vertex(mat, xBL, yBL, 0).color(cBL);
        builder.vertex(mat, xBR, yBR, 0).color(cBR);
        builder.vertex(mat, xTR, yTR, 0).color(cTR);
    }

    private static void pushSmoothRadialGlow(BufferBuilder builder, Matrix4f mat, float cx, float cy, float radius, int centerColor)
    {
        int maxAlpha = centerColor >>> 24;
        int rgb = centerColor & 0x00ffffff;

        float step = (radius * 2.0F) / GLOW_GRID;
        float startX = cx - radius;
        float startY = cy - radius;

        for (int r = 0; r < GLOW_GRID; r++)
        {
            float y1 = startY + r * step;

            for (int c = 0; c < GLOW_GRID; c++)
            {
                float wTL = GLOW_WEIGHTS[r][c];
                float wTR = GLOW_WEIGHTS[r][c + 1];
                float wBL = GLOW_WEIGHTS[r + 1][c];
                float wBR = GLOW_WEIGHTS[r + 1][c + 1];

                if (wTL > 0F || wTR > 0F || wBL > 0F || wBR > 0F)
                {
                    float x1 = startX + c * step;

                    int cTL = ((int) (maxAlpha * wTL) << 24) | rgb;
                    int cTR = ((int) (maxAlpha * wTR) << 24) | rgb;
                    int cBL = ((int) (maxAlpha * wBL) << 24) | rgb;
                    int cBR = ((int) (maxAlpha * wBR) << 24) | rgb;

                    pushQuad(builder, mat, x1, y1, step, step, cTL, cTR, cBL, cBR);
                }
            }
        }
    }

    private static class Star
    {
        float u;
        float v;
        float depth;
        float size;
        float twinkleSpeed;
        float twinklePhase;
        float baseAlpha;
        int color;
    }

    private static class HeroStar
    {
        float u;
        float v;
        float flareRadius;
        int color;
        float shimmerSpeed;
        float shimmerPhase;
    }

    private static class CosmicDust
    {
        float u;
        float v;
        float depth;
        float size;
        float wobbleSpeed;
        float wobblePhase;
        float wobbleAmp;
        float driftX;
        float driftY;
        float baseAlpha;
        int color;
    }
}
