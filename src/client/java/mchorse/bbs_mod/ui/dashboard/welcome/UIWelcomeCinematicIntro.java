package mchorse.bbs_mod.ui.dashboard.welcome;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.audio.SoundPlayer;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.utils.MathUtils;

import java.util.Random;

/**
 * Cinematic Prologue for the BBS 2.x Farewell Welcome Experience.
 *
 * <p>Plays a synchronized ambient music sequence with TheEnd.ogg, rendering a poetic
 * farewell quote with glowing text, warm rising ember motes, and an organic reveal
 * transition into the welcome wizard.</p>
 */
public class UIWelcomeCinematicIntro extends UIElement
{
    public static final Link THEEND_MUSIC = Link.assets("sounds/TheEnd.ogg");

    public static final float DURATION_FADE_IN_START = 0.8F;
    public static final float DURATION_FADE_IN_END = 3.0F;
    public static final float DURATION_HOLD_END = 6.0F;
    public static final float DURATION_FADE_OUT_END = 8.0F;
    public static final float DURATION_TOTAL = 9.5F;

    private static final int PARTICLE_COUNT = 52;

    private final Runnable onFinish;
    private final float[] particleX = new float[PARTICLE_COUNT];
    private final float[] particleY = new float[PARTICLE_COUNT];
    private final float[] particleSpeed = new float[PARTICLE_COUNT];
    private final float[] particleAlpha = new float[PARTICLE_COUNT];
    private final float[] particleSize = new float[PARTICLE_COUNT];
    private final int[] particleColor = new int[PARTICLE_COUNT];

    private long startTime;
    private boolean soundStarted;
    private boolean finished;

    public UIWelcomeCinematicIntro(Runnable onFinish)
    {
        super();

        this.onFinish = onFinish;
        this.startTime = System.currentTimeMillis();

        /* Initialize atmospheric warm stardust embers */
        Random rand = new Random(0x4E6F73L);

        for (int i = 0; i < PARTICLE_COUNT; i++)
        {
            this.particleX[i] = rand.nextFloat();
            this.particleY[i] = rand.nextFloat();
            this.particleSpeed[i] = 0.008F + rand.nextFloat() * 0.016F;
            this.particleAlpha[i] = 0.25F + rand.nextFloat() * 0.55F;
            this.particleSize[i] = 1.0F + rand.nextFloat() * 1.6F;
            this.particleColor[i] = rand.nextBoolean() ? 0xffcc88 : (rand.nextBoolean() ? 0xff9966 : 0xffeebb);
        }
    }

    public void start()
    {
        this.startTime = System.currentTimeMillis();
        this.finished = false;
        this.soundStarted = false;

        this.startSound();
    }

    private void startSound()
    {
        if (!this.soundStarted)
        {
            this.soundStarted = true;
            SoundPlayer player = BBSModClient.getSounds().playUnique(THEEND_MUSIC);

            if (player != null)
            {
                player.setVolume(0.2F);
            }
        }
    }

    public void skip()
    {
        if (this.finished)
        {
            return;
        }

        float elapsed = (System.currentTimeMillis() - this.startTime) / 1000F;

        if (elapsed < 0.4F)
        {
            return;
        }

        this.finished = true;
        this.startSound();

        if (this.onFinish != null)
        {
            this.onFinish.run();
        }
    }

    public float getRevealProgress()
    {
        float elapsed = (System.currentTimeMillis() - this.startTime) / 1000F;

        if (elapsed < DURATION_FADE_OUT_END)
        {
            return 0.0F;
        }

        float progress = (elapsed - DURATION_FADE_OUT_END) / (DURATION_TOTAL - DURATION_FADE_OUT_END);
        float clamped = MathUtils.clamp(progress, 0.0F, 1.0F);

        /* Smooth cosine ease for organic crossfade */
        return (float) (0.5D - 0.5D * Math.cos(clamped * Math.PI));
    }

    public boolean isFinished()
    {
        return this.finished;
    }

    @Override
    protected boolean subMouseClicked(UIContext context)
    {
        this.skip();

        return true;
    }

    @Override
    protected boolean subKeyPressed(UIContext context)
    {
        this.skip();

        return true;
    }

    @Override
    public void render(UIContext context)
    {
        if (this.parent != null)
        {
            this.area.copy(this.parent.area);
        }

        this.startSound();

        float elapsed = (System.currentTimeMillis() - this.startTime) / 1000F;

        if (elapsed >= DURATION_TOTAL && !this.finished)
        {
            this.finished = true;

            if (this.onFinish != null)
            {
                this.onFinish.run();
            }
        }

        /* 1. Calculate fade alpha for text and dark curtain with smooth cosine easing */
        float textAlpha = 0.0F;
        float curtainAlpha = 1.0F;

        if (elapsed < DURATION_FADE_IN_START)
        {
            textAlpha = 0.0F;
            curtainAlpha = 1.0F;
        }
        else if (elapsed < DURATION_FADE_IN_END)
        {
            float t = (elapsed - DURATION_FADE_IN_START) / (DURATION_FADE_IN_END - DURATION_FADE_IN_START);
            textAlpha = (float) (0.5D - 0.5D * Math.cos(t * Math.PI));
            curtainAlpha = 1.0F;
        }
        else if (elapsed < DURATION_HOLD_END)
        {
            textAlpha = 1.0F;
            curtainAlpha = 1.0F;
        }
        else if (elapsed < DURATION_FADE_OUT_END)
        {
            float t = (elapsed - DURATION_HOLD_END) / (DURATION_FADE_OUT_END - DURATION_HOLD_END);
            textAlpha = (float) (0.5D + 0.5D * Math.cos(t * Math.PI));
            curtainAlpha = 1.0F;
        }
        else
        {
            textAlpha = 0.0F;
            float t = MathUtils.clamp((elapsed - DURATION_FADE_OUT_END) / (DURATION_TOTAL - DURATION_FADE_OUT_END), 0.0F, 1.0F);
            curtainAlpha = (float) (0.5D + 0.5D * Math.cos(t * Math.PI));
        }

        /* 2. Render pitch black backdrop curtain */
        if (curtainAlpha > 0.001F)
        {
            int blackColor = ((int) (0xFF * curtainAlpha) << 24) | 0x07060c;
            context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(), blackColor);
        }

        /* 3. Render drifting memory stardust motes */
        if (curtainAlpha > 0.05F)
        {
            this.renderStardust(context, elapsed, curtainAlpha);
        }

        /* 4. Render central nostalgic farewell quote with smooth zoom & glow */
        if (textAlpha > 0.015F)
        {
            this.renderQuote(context, elapsed, textAlpha);
        }

        /* 5. Render subtle skip prompt */
        if (elapsed > 1.6F && curtainAlpha > 0.2F)
        {
            this.renderSkipPrompt(context, elapsed, curtainAlpha);
        }

        super.render(context);
    }

    private void renderStardust(UIContext context, float elapsed, float curtainAlpha)
    {
        int w = this.area.w;
        int h = this.area.h;

        for (int i = 0; i < PARTICLE_COUNT; i++)
        {
            float px = (this.particleX[i] * w + this.area.x);
            float py = (this.particleY[i] - elapsed * this.particleSpeed[i]) % 1.0F;

            if (py < 0)
            {
                py += 1.0F;
            }

            float posY = this.area.y + py * h;
            float pAlpha = this.particleAlpha[i] * curtainAlpha * (0.5F + 0.5F * (float) Math.sin(elapsed * 1.8F + i));
            int color = ((int) (pAlpha * 255) << 24) | this.particleColor[i];

            float sz = this.particleSize[i];
            context.batcher.box(px, posY, px + sz, posY + sz, color);
        }
    }

    private void renderQuote(UIContext context, float elapsed, float textAlpha)
    {
        if (textAlpha <= 0.015F)
        {
            return;
        }

        int alpha = (int) (textAlpha * 255);

        if (alpha < 4)
        {
            return;
        }

        String quote = UIKeys.WELCOME_INTRO_QUOTE.get();
        FontRenderer font = context.batcher.getFont();

        int textW = font.getWidth(quote);
        int textH = font.getHeight();
        int cx = this.area.mx();
        int cy = this.area.my();

        int mainColor = (alpha << 24) | 0xffeedd;
        context.batcher.text(quote, cx - textW / 2, cy - textH / 2, mainColor, false);
    }

    private void renderSkipPrompt(UIContext context, float elapsed, float curtainAlpha)
    {
        float promptAlpha = Math.min(1.0F, (elapsed - 1.6F) / 1.5F) * 0.35F * curtainAlpha;
        int alpha = (int) (promptAlpha * 255);

        if (alpha < 4)
        {
            return;
        }

        int color = (alpha << 24) | 0xa09088;
        String skipText = UIKeys.WELCOME_INTRO_SKIP.get();
        FontRenderer font = context.batcher.getFont();
        int textW = font.getWidth(skipText);

        context.batcher.text(skipText, this.area.mx() - textW / 2, this.area.ey() - 24, color, false);
    }
}
